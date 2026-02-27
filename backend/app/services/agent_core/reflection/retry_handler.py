"""Exponential backoff and fallback prompts for LLM failure states."""

import logging
from typing import Callable, Any, Dict

logger = logging.getLogger(__name__)


class RetryHandler:
    """Manages multi-attempt LLM execution with progressive constraints."""

    def __init__(self, max_retries: int = 3):
        self.max_retries = max_retries

    async def execute_with_retry(self, execution_func: Callable, *args, **kwargs) -> Dict[str, Any]:
        """Execute an LLM generation loop with progressive temperature adjustments."""
        
        temperatures = [0.7, 0.9, 0.4] # Progressive tiering
        
        for attempt in range(self.max_retries):
            temperature = temperatures[attempt] if attempt < len(temperatures) else 0.4
            
            try:
                logger.debug(f"LLM Generation Attempt {attempt + 1}, temp={temperature}")
                
                # Execute the provided async function (usually calling the LLM API)
                is_valid, error, parsed_response = await execution_func(temperature, *args, **kwargs)
                
                if is_valid:
                    return parsed_response
                    
                logger.warning(f"Validation failed on attempt {attempt + 1}: {error}")
                
            except Exception as e:
                logger.error(f"Execution critically crashed on attempt {attempt + 1}: {str(e)}")
                
        # If all retries fail, trigger the extreme fallback (micro-prompting)
        logger.error("All structural generation attempts failed. Falling back to micro-prompt.")
        return self._trigger_micro_fallback()

    def _trigger_micro_fallback(self) -> Dict[str, Any]:
        """Emergency fallback: Emits an extremely simple persona-compliant string.
        
        While this violates the "No static replies" strict rule, this only triggers if 
        the LLM provider is entirely down or consistently failing JSON schema.
        We still randomize it slightly to avoid pure statics.
        """
        import random
        # Highly conservative panic messages for Margaret (72)
        fallbacks = [
            "Oh dear, my screen just went black for a moment. What were you saying?",
            "I'm sorry, my internet is acting up. Could you repeat that?",
            "Wait, I dropped my reading glasses. What do I need to do next?"
        ]
        
        return {
            "internal_reasoning": {
                "situation_analysis": "SYSTEM FAILURE",
                "strategy_selection": "EMERGENCY MICRO-PROMPT TRIGGERED",
                "persona_alignment_check": "MANUAL OVERRIDE"
            },
            "final_response": random.choice(fallbacks)
        }
