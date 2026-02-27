"""Schemas for the Scam Detection Engine."""

from pydantic import BaseModel, ConfigDict
from typing import List


class ScamSignal(BaseModel):
    """Result of scam detection analysis with multi-layer confidence routing.
    
    This matches the specific output schema for the detector:
    {
      "scamDetected": true/false,
      "confidenceScore": 0.87,
      "riskLevel": "low | medium | high | critical",
      "explanations": ["High urgency language detected", ...]
    }
    """
    
    model_config = ConfigDict(populate_by_name=True)

    scamDetected: bool
    confidenceScore: float
    riskLevel: str
    explanations: List[str]
    
    # Backward compatibility with existing codebase
    @property
    def is_scam(self) -> bool:
        return self.scamDetected
        
    @property
    def confidence(self) -> float:
        return self.confidenceScore
