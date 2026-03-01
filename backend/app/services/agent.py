"""Agent Controller and LLM-backed conversation engine.

Coordinates scam detection, session management, and response generation.
Delegates to the dynamic AgentOrchestrator for response generation.
"""

import logging
from typing import List, Dict, Any

from app.config.settings import settings
from app.core.llm import get_llm_provider
from app.models.schemas import Message
from app.services.detection import detect_scam
from app.services.extraction import extract_intelligence, merge_intel_buffer
from app.services.session import SessionState, get_session_store
from app.services.agent_core.orchestrator import AgentOrchestrator
from app.services.events import get_event_bus
from app.services.callback import fire_final_callback

logger = logging.getLogger(__name__)

# Get the LLM provider (Ollama by default, configurable via factory)
_provider = get_llm_provider()

async def _llm_generation_wrapper(messages: List[Dict[str, str]], temperature: float) -> str:
    """Wrapper function to adapt the dynamic PromptBuilder ChatML to LangChain Provider."""
    from langchain_core.messages import SystemMessage, HumanMessage, AIMessage

    lc_messages = []
    for m in messages:
        role = m.get("role")
        content = m.get("content", "")
        if role == "system":
            lc_messages.append(SystemMessage(content=content))
        elif role == "user":
            lc_messages.append(HumanMessage(content=content))
        elif role == "assistant":
            lc_messages.append(AIMessage(content=content))

    response = await _provider.generate_response(lc_messages, temperature=temperature)
    return str(response)

# Initialize the dynamic conversation brain
_orchestrator = AgentOrchestrator(llm_generation_func=_llm_generation_wrapper)


class AgentController:
    """Controls the flow of honeypot conversations."""

    def __init__(self) -> None:
        self._store = get_session_store()
        self._event_bus = get_event_bus()

    def _get_missing_entities(self, intel_buffer: Dict[str, Any]) -> List[str]:
        """Determine what intelligence is still missing for the Orchestrator context."""
        expected = ["UPI", "Bank Account", "Phone Number", "Phishing Link"]
        found = []
        if intel_buffer.get("upiIds"): found.append("UPI")
        if intel_buffer.get("bankAccounts"): found.append("Bank Account")
        if intel_buffer.get("phoneNumbers"): found.append("Phone Number")
        if intel_buffer.get("phishingLinks"): found.append("Phishing Link")

        return [e for e in expected if e not in found]

    async def process_message(
        self,
        session_id: str,
        message: Message,
        conversation_history: List[Message],
    ) -> str:
        """Process an incoming message and generate a response."""
        
        if not settings.FEATURE_ENGAGEMENT_ENABLED:
            logger.info("Engagement engine disabled via feature flag.")
            return "System currently unavailable."

        # 1. Get or create session
        session = await self._store.get(session_id)
        if session is None:
            session = SessionState(session_id=session_id)
            logger.info(f"Created new session: {session_id}")

        if session.state in ["completed", "safe"]:
            logger.info(f"Session {session_id} is already {session.state}")
            return "This session has concluded."

        # 2. Run scam detection ONLY if in initial state
        if session.state in ["idle", "detecting"]:
            scam_result = detect_scam(message.text, previous_session_score=session.scam_score)
            
            if scam_result.scamDetected:
                session.scam_score = max(session.scam_score, scam_result.confidenceScore)
                session.is_scam = True

            # State transition
            session.advance_state(
                scam_detected=session.is_scam,
                max_turns=settings.MAX_SESSION_MESSAGES
            )
            
            if session.state == "engaging" and session.turn_number == 0:
                logger.info(f"Scam detected for session {session_id}, agent activated")
                await self._event_bus.publish("scam.detected", {
                    "session_id": session_id,
                    "confidence_score": session.scam_score,
                    "sender": message.sender,
                    "text": message.text
                })

        if session.state == "safe":
             await self._store.save(session)
             return "Normal message received. Evaluated as safe."

        # 3. Extract intelligence from message
        intel = extract_intelligence(message.text)
        session.intel_buffer = merge_intel_buffer(session.intel_buffer, intel)

        # 4. Construct Dynamic LLM Context
        full_history = list(conversation_history) + [message]
        chatml_history = []
        for m in full_history:
            role = "user" if m.sender != "agent" else "assistant"
            chatml_history.append({"role": role, "content": m.text})

        session_context = {
            "history": chatml_history,
            "message_count": session.turn_number,
            "missing_entities": self._get_missing_entities(session.intel_buffer),
            "max_messages": settings.MAX_SESSION_MESSAGES,
        }

        detection_state = {
            "confidenceScore": session.scam_score,
            "riskLevel": "high" if session.scam_score > 0.8 else "medium",
        }

        # 5. Generate dynamic agent response via Orchestrator
        try:
            reply = await _orchestrator.generate_response(
                persona_id=settings.AGENT_DEFAULT_PERSONA,
                session_context=session_context,
                detection_state=detection_state,
            )
        except Exception as e:
            logger.error(f"Orchestrator catastrophic failure: {e}")
            reply = _orchestrator.retry_handler._trigger_micro_fallback()["final_response"]

        # 6. Update turn count and re-evaluate state
        session.turn_number += 1
        session.advance_state(
            scam_detected=session.is_scam,
            max_turns=settings.MAX_SESSION_MESSAGES
        )

        # 7. Publish Turn Event
        await self._event_bus.publish("engagement.turn", {
            "session_id": session_id,
            "turn_number": session.turn_number,
            "sender": message.sender,
            "reply": reply,
            "intel_buffer": session.intel_buffer
        })

        # 8. Check for completion path
        if session.state == "completing":
            logger.info(f"Session {session_id} completing, firing callback")
            await self._event_bus.publish("engagement.completed", {
                "session_id": session_id,
                "reason": "max_turns_or_intent"
            })
            
            # Fire invariant callback
            await fire_final_callback(session)
            
            # Advance to final state regardless of callback outcome (for now)
            session.state = "completed"

        # 9. Save session
        await self._store.save(session)

        return reply


# Global controller instance
_controller: AgentController | None = None


def get_controller() -> AgentController:
    """Get or create the controller singleton."""
    global _controller
    if _controller is None:
        _controller = AgentController()
    return _controller
