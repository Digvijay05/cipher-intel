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

logger = logging.getLogger(__name__)

# Get the LLM provider (Ollama by default, configurable via factory)
_provider = get_llm_provider()

async def _llm_generation_wrapper(messages: List[Dict[str, str]], temperature: float) -> str:
    """Wrapper function to adapt the dynamic PromptBuilder ChatML to LangChain Provider.

    The Orchestrator produces standard arrays [{"role": "system", "content": "..."}].
    Our core provider's generate_response() expects LangChain message objects.
    """
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
    """Controls the flow of honeypot conversations.

    Responsibilities:
    - Decide if scam is detected (activate agent)
    - Manage session state
    - Determine stop conditions
    - Generate appropriate responses dynamically via Orchestrator
    """

    def __init__(self) -> None:
        self._store = get_session_store()

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
        """Process an incoming message and generate a response.

        Args:
            session_id: Unique session identifier.
            message: The incoming message.
            conversation_history: Previous messages in the conversation.

        Returns:
            The response string to send back.
        """
        # 1. Get or create session
        session = await self._store.get(session_id)
        if session is None:
            session = SessionState(session_id=session_id)
            logger.info(f"Created new session: {session_id}")

        # 2. Run scam detection
        scam_result = detect_scam(message.text, previous_session_score=session.scam_score)
        logger.debug(f"Scam detection: {scam_result}")

        # 3. Update session state
        session.message_count += 1
        if scam_result.scamDetected:
            session.scam_score = max(session.scam_score, scam_result.confidenceScore)
            session.is_scam = True
            session.agent_active = True
            logger.info(f"Scam detected for session {session_id}, agent activated")

        # 4. Extract intelligence from message
        intel = extract_intelligence(message.text)
        session.intel_buffer = merge_intel_buffer(session.intel_buffer, intel)
        logger.debug(f"Intel buffer updated: {session.intel_buffer}")

        # 5. Check stop conditions
        if session.message_count >= settings.MAX_SESSION_MESSAGES:
            logger.info(
                f"Session {session_id} reached max messages ({settings.MAX_SESSION_MESSAGES})"
            )

        # 6. Construct Dynamic LLM Context
        full_history = list(conversation_history) + [message]
        chatml_history = []
        for m in full_history:
            role = "user" if m.sender != "agent" else "assistant"
            chatml_history.append({"role": role, "content": m.text})

        session_context = {
            "history": chatml_history,
            "message_count": session.message_count,
            "missing_entities": self._get_missing_entities(session.intel_buffer),
            "max_messages": settings.MAX_SESSION_MESSAGES,
        }

        detection_state = {
            "confidenceScore": scam_result.confidenceScore,
            "riskLevel": scam_result.riskLevel,
        }

        # 7. Generate dynamic agent response via Orchestrator
        try:
            reply = await _orchestrator.generate_response(
                persona_id=settings.AGENT_DEFAULT_PERSONA,
                session_context=session_context,
                detection_state=detection_state,
            )
        except Exception as e:
            logger.error(f"Orchestrator catastrophic failure: {e}")
            reply = _orchestrator.retry_handler._trigger_micro_fallback()["final_response"]

        # 8. Save session
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
