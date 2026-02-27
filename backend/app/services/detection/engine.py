"""Confidence Aggregation & Scoring Engine.

Orchestrates the multi-layer pipeline and outputs the standardized ScamSignal.
"""

from typing import List, Dict, Any
import asyncio

from .schemas import ScamSignal
from .layer1_heuristics import HeuristicsLayer
from .layer2_behavioral import BehavioralNLPLayer
from .layer3_semantic import SemanticContextLayer


class ScamDetectorEngine:
    """Core aggregation engine utilizing ensemble weighting."""

    def __init__(self):
        self.l1 = HeuristicsLayer()
        self.l2 = BehavioralNLPLayer()
        self.l3 = SemanticContextLayer()
        
        # Ensemble weights
        self.W1 = 0.55   # Fast Deterministic heuristics (High Priority)
        self.W2 = 0.45   # Statistical Behavioral NLP
        self.W3 = 0.25   # Deep Semantic Transformer (Booster / Tie-breaker)

    def map_risk_level(self, confidence: float) -> str:
        """Map raw float confidence into standard risk tier."""
        if confidence >= 0.85: return "critical"
        if confidence >= 0.65: return "high"
        if confidence >= 0.45: return "medium"
        return "low"

    def detect_scam(self, text: str, previous_session_score: float = 0.0, alpha: float = 0.6) -> ScamSignal:
        """Run the multi-layer pipeline over the incoming text.
        
        Args:
            text: Message to analyze.
            previous_session_score: Prior historical risk (for temporal decay).
            alpha: Decay parameter (default 0.6).
            
        Returns:
            Structured ScamSignal matching the strict output schema.
        """
        # Execute layers (could be async.gather in highly concurrent systems)
        res_l1 = self.l1.analyze(text)
        res_l2 = self.l2.analyze(text)
        res_l3 = self.l3.analyze(text)
        
        # Aggregate logic
        curr_score = (self.W1 * res_l1["score"]) + (self.W2 * res_l2["score"]) + (self.W3 * res_l3["score"])
        
        # Explanation building
        explanations = []
        explanations.extend(res_l1["explanations"])
        explanations.extend(res_l2["explanations"])
        explanations.extend(res_l3["explanations"])

        # Session Decay Formula (C_final = max(C_curr, alpha * C_prev + (1-alpha) * C_curr))
        historical_bleed = (alpha * previous_session_score) + ((1 - alpha) * curr_score)
        
        final_score = max(curr_score, historical_bleed)

        # Truncate precision and cap at 1.00
        final_confidence = min(1.0, round(final_score, 2))
        risk = self.map_risk_level(final_confidence)
        
        if final_confidence > curr_score and final_confidence > 0.45:
            explanations.append(f"Context: Session risk elevated from semantic history ({final_confidence})")
            
        return ScamSignal(
            scamDetected=final_confidence >= 0.50,
            confidenceScore=final_confidence,
            riskLevel=risk,
            explanations=explanations
        )
