"""Layer 1: Fast Heuristics (<10ms latency).

Uses regex, structural entropy, and dictionary lookups to natively flag
definitive scam entities.
"""

import re
from dataclasses import dataclass
from typing import List, Pattern


@dataclass
class HeuristicRule:
    name: str
    pattern: Pattern[str]
    weight: float
    description: str


# Improved Rule Dictionary (Fast blocklist and regexes)
HEURISTIC_RULES: List[HeuristicRule] = [
    # Payment / Redirection
    HeuristicRule(
        name="upi_id",
        pattern=re.compile(r"[a-zA-Z0-9._-]+@(ybl|paytm|okaxis|oksbi|okhdfcbank|axl|upi|ibl)", re.IGNORECASE),
        weight=0.4,
        description="L1: UPI ID blocklist entity found",
    ),
    HeuristicRule(
        name="upi_link",
        pattern=re.compile(r"upi://pay\?", re.IGNORECASE),
        weight=0.5,
        description="L1: Deep-link payment redirection",
    ),
    # Institutional Impersonation (High Confidence)
    HeuristicRule(
        name="bank_impersonation",
        pattern=re.compile(r"\b(sbi|hdfc|icici|axis|rbi|reserve\s*bank|bank\s*of\s*india)\s*(bank|customer\s*care|support)?\b", re.IGNORECASE),
        weight=0.3,
        description="L1: Banking institution impersonation",
    ),
    HeuristicRule(
        name="govt_impersonation",
        pattern=re.compile(r"\b(income\s*tax|it\s*department|customs|cyber\s*cell|police|government)\b", re.IGNORECASE),
        weight=0.4,
        description="L1: Authority/Government impersonation",
    ),
    # Critical extraction commands
    HeuristicRule(
        name="otp_request",
        pattern=re.compile(r"\b(otp|one\s*time\s*password|verification\s*code|pin|cvv)\b", re.IGNORECASE),
        weight=0.45,
        description="L1: PII/OTP extraction attempt",
    ),
    HeuristicRule(
        name="password_request",
        pattern=re.compile(r"\b(password|login\s*credentials?|username\s*and\s*password)\b", re.IGNORECASE),
        weight=0.45,
        description="L1: Credential theft attempt",
    ),
    HeuristicRule(
        name="bank_details",
        pattern=re.compile(r"\b(bank\s*details?|account\s*number|ifsc|card\s*number|atm\s*pin)\b", re.IGNORECASE),
        weight=0.45,
        description="L1: Bank details request",
    ),
    HeuristicRule(
        name="lottery_scam",
        pattern=re.compile(r"\b(lottery|winner|prize|won|congratulations.*claim|lucky\s*draw)\b", re.IGNORECASE),
        weight=0.45,
        description="L1: Lottery/Prize scam pattern",
    ),
    HeuristicRule(
        name="job_scam",
        pattern=re.compile(r"\b(earn.*from\s*home|daily\s*income|part\s*time.*earn)\b", re.IGNORECASE),
        weight=0.35,
        description="L1: Employment/Work-from-home scam pattern",
    ),
    HeuristicRule(
        name="kyc_scam",
        pattern=re.compile(r"\b(kyc.*expir|update.*kyc|verify.*kyc|pan.*link)\b", re.IGNORECASE),
        weight=0.40,
        description="L1: KYC verification/update urgency",
    ),
    # Obfuscation anomalies
    HeuristicRule(
        name="obfuscated_text",
        pattern=re.compile(r"([a-zA-Z]\.[a-zA-Z]\.[a-zA-Z]\.[a-zA-Z])|([a-zA-Z]![a-zA-Z])"),
        weight=0.3,
        description="L1: Obfuscation anomaly detected (filter evasion attempt)",
    ),
    # Link blocks
    HeuristicRule(
        name="suspicious_url",
        pattern=re.compile(r"https?://(?!www\.(google|microsoft|apple|amazon|facebook|twitter|instagram)\.com)[^\s]+\.(xyz|tk|ml|ga|cf|gq|top|click|link|info)/", re.IGNORECASE),
        weight=0.45,
        description="L1: Suspicious TLD URL blocklist match",
    ),
    HeuristicRule(
        name="shortened_url",
        pattern=re.compile(r"https?://(bit\.ly|tinyurl|t\.co|goo\.gl|ow\.ly|is\.gd|buff\.ly)/[^\s]+", re.IGNORECASE),
        weight=0.35,
        description="L1: Obfuscated URL redirection",
    ),
]


class HeuristicsLayer:
    """Detects deterministic technical footprint anomalies."""

    def analyze(self, text: str) -> dict:
        """Run L1 rules on the text and aggregate confidence."""
        matched_descriptions = []
        score = 0.0

        for rule in HEURISTIC_RULES:
            if rule.pattern.search(text):
                score += rule.weight
                matched_descriptions.append(rule.description)

        # Soft cap L1 score at 1.0 but realistic max is bound by W1 in ensemble
        capped_score = min(1.0, score)
        return {
            "score": capped_score,
            "explanations": matched_descriptions
        }
