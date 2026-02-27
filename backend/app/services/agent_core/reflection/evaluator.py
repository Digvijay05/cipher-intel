"""Post-generation critique and schema validation logic."""

import json
from pydantic import ValidationError
from typing import Dict, Any, Tuple

from ..prompts.structural import AgentResponse


class ReflectionEvaluator:
    """Validates the structure and semantic safety of LLM outputs."""

    def evaluate(self, raw_llm_output: str) -> Tuple[bool, str, Dict[str, Any]]:
        """Validate the LLM response against strict schemas and static-reply checks.
        
        Returns:
            Tuple: (is_valid: bool, error_msg: str, parsed_data: dict)
        """
        parsed_json = {}
        
        try:
            # 1. Parse JSON
            parsed_json = json.loads(raw_llm_output)
            
            # 2. Pydantic validation (Strict Schema matching)
            agent_response = AgentResponse(**parsed_json)
            
            # 3. Anti-Static Response Enforcement
            if not self._check_dynamic_liveness(agent_response):
                return False, "EVAL_FAIL: Reasoning block too shallow or generic.", parsed_json
                
            return True, "", agent_response.model_dump()
            
        except json.JSONDecodeError as e:
            return False, f"EVAL_FAIL: Invalid JSON format. {str(e)}", parsed_json
        except ValidationError as e:
            return False, f"EVAL_FAIL: Missing required schema fields. {str(e)}", parsed_json
        except Exception as e:
            return False, f"EVAL_FAIL: Unexpected reflection error. {str(e)}", parsed_json

    def _check_dynamic_liveness(self, response: AgentResponse) -> bool:
        """Ensure the LLM actually reasoned and didn't hallucinate a template."""
        # Check if the internal reasoning holds actual substance
        if len(response.internal_reasoning.situation_analysis) < 10:
            return False
        if len(response.internal_reasoning.strategy_selection) < 10:
            return False
            
        final = response.final_response.lower()
        
        # Hard blocklists of generic bot fallbacks
        generic_templates = [
            "as an ai", "i cannot assist", "i do not understand", 
            "sorry, i am", "i am an ai"
        ]
        
        for template in generic_templates:
            if template in final:
                return False
                
        return True
