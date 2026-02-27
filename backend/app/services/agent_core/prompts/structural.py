"""Pydantic schemas enforcing strict structured output from the LLM."""

from pydantic import BaseModel, Field


class InternalReasoning(BaseModel):
    """The hidden cognitive steps the agent takes before generating a reply."""
    
    situation_analysis: str = Field(
        description="Brief analysis of the scammer's current tactics (e.g., urgency, threats, financial requests)."
    )
    strategy_selection: str = Field(
        description="The chosen conversation tactic to prolong engagement without falling for the scam."
    )
    persona_alignment_check: str = Field(
        description="A validation step ensuring the strategy fits the persona's age, tech-literacy, and traits."
    )


class AgentResponse(BaseModel):
    """The strictly enforced JSON structure the LLM must return."""
    
    internal_reasoning: InternalReasoning = Field(
        description="The cognitive reasoning block (never shown to the scammer)."
    )
    final_response: str = Field(
        description="The actual string response sent back to the scammer. Must be highly conversational and believable."
    )
