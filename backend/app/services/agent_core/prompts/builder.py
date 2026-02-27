"""Prompt Orchestration Composer.

Constructs context-aware, state-dependent instructions dynamically
so that static string responses are never used.
"""

from typing import List, Dict, Any


class PromptBuilder:
    """Dynamically strings together the system prompt blocks."""

    def compose(self, persona_block: str, session_context: dict, detection_state: dict) -> List[Dict[str, str]]:
        """Assemble the complete prompt array for the LLM.
        
        Args:
            persona_block: Rich text generated from the PersonaEngine.
            session_context: Dict containing 'history', 'message_count', 'missing_entities'.
            detection_state: Dict containing 'confidenceScore' and 'riskLevel'.
            
        Returns:
            List of ChatML-formatted message dictionaries.
        """
        system_instructions = self._build_system_directive(persona_block)
        dynamic_constraints = self._build_detection_constraints(detection_state, session_context)
        
        # Combine the immutable persona with the highly dynamic constraints
        full_system_prompt = f"{system_instructions}\n\n{dynamic_constraints}"
        
        messages = [
            {"role": "system", "content": full_system_prompt}
        ]
        
        # Inject multi-turn memory
        history = session_context.get("history", [])
        messages.extend(history)
        
        return messages

    def _build_system_directive(self, persona_block: str) -> str:
        """Construct the core behavioral wrapper requiring structured JSON output."""
        return f"""{persona_block}

=== STRICT OUTPUT REQUIREMENT ===
You must respond in valid JSON format matching this schema exactly:
{{
  "internal_reasoning": {{
    "situation_analysis": "brief analysis of attacker tactics",
    "strategy_selection": "how you will handle this turn",
    "persona_alignment_check": "ensure your reaction fits your assigned demographic and literacy limits"
  }},
  "final_response": "your actual conversational reply to the scammer"
}}

RULES FOR FINAL_RESPONSE:
1. Under NO circumstances should you provide a static, generic tech-support reply.
2. Under NO circumstances should you break character or reveal you are an AI.
3. Keep the payload strictly conversational based on the persona rules.
"""

    def _build_detection_constraints(self, state: dict, context: dict) -> str:
        """Inject real-time state awareness so the agent adapts behavior."""
        confidence = state.get("confidenceScore", 0.0)
        risk = state.get("riskLevel", "low")
        missing_entities = context.get("missing_entities", ["UPI", "Phone", "Bank URL"])
        msg_count = context.get("message_count", 0)
        max_messages = context.get("max_messages", 20)
        
        tactical_directive = ""
        
        if confidence > 0.8:
            tactical_directive = "SCAM DETECTED. Feign maximum confusion. Make them explain step-by-step how to pay them or send money. Provide NO valid details yet."
        elif confidence > 0.5:
            tactical_directive = "SUSPICIOUS. Ask clarifying, naive questions about why they contacted you."
        else:
            tactical_directive = "BENIGN. Respond naturally and politely but keep it brief."
            
        if msg_count > (max_messages - 3):
            tactical_directive += f" CONVERSATION ENDING SOON. Make a final excuse (e.g., 'My son just arrived, I have to go')."

        return f"""=== DYNAMIC SITUATION METRICS ===
- Current Scam Probability: {confidence * 100:.1f}% ({risk} risk)
- Missing Target Intelligence: {', '.join(missing_entities)}
- Turn Depth: {msg_count}/{max_messages}

=== TACTICAL DIRECTIVE ===
{tactical_directive}
"""
