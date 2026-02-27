"""Multi-layer scam detection architecture.

Provides an ensemble classification scoring engine for detecting
social engineering and financial fraud payloads.
"""

from .schemas import ScamSignal
from .engine import ScamDetectorEngine
from .layer1_heuristics import HeuristicsLayer, HEURISTIC_RULES

# Singleton instance of the engine
_engine = ScamDetectorEngine()

def detect_scam(text: str, previous_session_score: float = 0.0) -> ScamSignal:
    """Analyze text for scam intent using the multi-layer pipeline.
    
    Args:
        text: The message text to analyze.
        previous_session_score: Prior historical risk for session decay tracking.
        
    Returns:
        Structured ScamSignal (scamDetected, confidenceScore, riskLevel, explanations)
    """
    return _engine.detect_scam(text, previous_session_score)

__all__ = ["detect_scam", "ScamSignal", "ScamDetectorEngine", "HeuristicsLayer", "HEURISTIC_RULES"]
