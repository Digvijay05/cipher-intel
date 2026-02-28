import logging
import json
import asyncio
from typing import Dict, Any, List

from app.config.settings import settings

from .persona.engine import PersonaEngine
from .prompts.builder import PromptBuilder
from .memory.summarizer import MemorySummarizer
from .reflection.evaluator import ReflectionEvaluator
from .reflection.retry_handler import RetryHandler

logger = logging.getLogger(__name__)


class AgentOrchestrator:
    """The central brain replacing static/scripted conversation loops."""

    def __init__(self, llm_generation_func):
        """
        Args:
            llm_generation_func: An async callable that accepts (messages, temperature) 
                                 and returns a raw string from the LLM.
        """
        self.persona_engine = PersonaEngine()
        self.prompt_builder = PromptBuilder()
        self.memory_summarizer = MemorySummarizer()
        self.evaluator = ReflectionEvaluator()
        self.retry_handler = RetryHandler(max_retries=settings.AGENT_MAX_RETRIES)
        self.llm_func = llm_generation_func

    async def generate_response(
        self, 
        persona_id: str, 
        session_context: Dict[str, Any], 
        detection_state: Dict[str, Any]
    ) -> str:
        """Fully orchestrates prompt building, LLM execution, reflection, and retries."""
        # 1. Load the specific persona traits
        persona_block = self.persona_engine.build_system_prompt_segment(persona_id)
        
        # 2. Compress the message list if the conversation is dragging on
        compressed_history = self.memory_summarizer.process_history(session_context.get("history", []))
        session_context["history"] = compressed_history
        
        # 3. Assemble the dynamic ChatML prompt array
        messages = self.prompt_builder.compose(
            persona_block=persona_block,
            session_context=session_context,
            detection_state=detection_state
        )
        
        # 4. Prepare the strictly typed completion function for the retry handler
        async def generation_attempt(temperature: float):
            try:
                raw_response = await asyncio.wait_for(
                    self.llm_func(messages=messages, temperature=temperature), 
                    timeout=settings.LLM_GENERATION_TIMEOUT_SECONDS
                )
                
                # Strip out potential markdown code blocks 
                raw_response = raw_response.strip()
                if raw_response.startswith("```json"):
                    raw_response = raw_response[7:]
                if raw_response.endswith("```"):
                    raw_response = raw_response[:-3]
                    
                return self.evaluator.evaluate(raw_response)
                
            except asyncio.TimeoutError:
                return False, "EVAL_FAIL: LLM Timeout generated.", {}
            except Exception as e:
                return False, f"EVAL_FAIL: {str(e)}", {}

        # 5. Execute generation loop with reflection and varying temperatures
        validated_schema = await self.retry_handler.execute_with_retry(generation_attempt)
        
        # 6. Extract the verified conversational string
        final_reply = validated_schema.get("final_response", "Pardon me, my screen just went black. What were you saying?")
        
        logger.info(f"Generated dynamic reply. Internal reasoning: {validated_schema.get('internal_reasoning')}")
        return final_reply
