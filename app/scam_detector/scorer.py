"""Scoring system for scam detection.

Aggregates rule matches into a final scam signal.
"""

from dataclasses import dataclass, field
from typing import List

from app.scam_detector.rules import ScamRule, match_rules

# Threshold for triggering scam detection
SCAM_THRESHOLD: float = 0.5


@dataclass
class ScamSignal:
    """Result of scam detection analysis."""

    is_scam: bool
    confidence: float
    matched_rules: List[str] = field(default_factory=list)


def calculate_score(matched: List[ScamRule]) -> float:
    """Calculate aggregate scam score from matched rules.

    Uses diminishing returns: each additional match adds less.
    Score is capped at 1.0.

    Args:
        matched: List of matched ScamRule objects.

    Returns:
        Float score between 0.0 and 1.0.
    """
    if not matched:
        return 0.0

    total = sum(rule.weight for rule in matched)
    # Apply soft cap using tanh-like function
    return min(1.0, total)


def detect_scam(text: str) -> ScamSignal:
    """Analyze text for scam intent using regex rules.

    Args:
        text: The message text to analyze.

    Returns:
        ScamSignal with is_scam boolean, confidence score, and matched rules.
    """
    matched = match_rules(text)
    score = calculate_score(matched)

    return ScamSignal(
        is_scam=score >= SCAM_THRESHOLD,
        confidence=round(score, 2),
        matched_rules=[rule.name for rule in matched],
    )
