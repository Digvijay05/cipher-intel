import re
from dataclasses import dataclass
from typing import List, Pattern

@dataclass
class HeuristicRule:
    name: str
    pattern: Pattern[str]
    weight: float
    description: str

HEURISTIC_RULES: List[HeuristicRule] = [
    HeuristicRule(
        name="payment_redirection",
        pattern=re.compile(r"upi://pay\?", re.IGNORECASE),
        weight=0.5,
        description="L1: Deep-link payment redirection"
    ),
    HeuristicRule(
        name="institutional_impersonation",
        pattern=re.compile(r"\b(sbi|hdfc|icici|axis|income\s*tax|police|government)\b", re.IGNORECASE),
        weight=0.4,
        description="L1: Authority/Institutional impersonation"
    ),
    HeuristicRule(
        name="critical_extraction",
        pattern=re.compile(r"\b(otp|password|pin|cvv|bank\s*details)\b", re.IGNORECASE),
        weight=0.45,
        description="L1: PII/OTP extraction attempt"
    ),
    HeuristicRule(
        name="urgency_scam",
        pattern=re.compile(r"\b(account.*suspended|verify.*identity|lottery|winner|claim|urgent)\b", re.IGNORECASE),
        weight=0.45,
        description="L1: Urgency/Suspension/Prize lure"
    ),
    HeuristicRule(
        name="suspicious_url",
        pattern=re.compile(r"https?://[^\s]+\.(xyz|tk|ml|ga|cf|gq|top|click|link)/|https?://(bit\.ly|tinyurl)/", re.IGNORECASE),
        weight=0.45,
        description="L1: Suspicious or shortened URL"
    ),
]

class RuleEngine:
    """Fast heuristics fallback or booster."""
    
    def analyze(self, text: str) -> dict:
        matched_descriptions = []
        score = 0.0

        for rule in HEURISTIC_RULES:
            if rule.pattern.search(text):
                score += rule.weight
                matched_descriptions.append(rule.description)

        capped_score = min(1.0, score)
        return {
            "score": capped_score,
            "explanations": matched_descriptions,
            "scam_detected": capped_score >= 0.45
        }
