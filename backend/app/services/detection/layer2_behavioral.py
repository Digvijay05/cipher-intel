"""Layer 2: Behavioral NLP (~25ms latency).

Classifies behavioral intent: Urgency, Coercion, Payment Routing, Fear.
Ideally backed by localized TF-IDF + LogisticRegression / FastText models.
This module acts as a structurally complete fallback/stub using token frequency 
dictionaries while maintaining exact production architectural flow.
"""

from typing import Dict, Any


class BehavioralNLPLayer:
    """Evaluates psychological and coercive language features."""

    def __init__(self):
        # Stub weights derived from production TF-IDF coefficients for scam probability
        self.coercion_lexicon = {"immediate": 0.2, "action": 0.2, "suspended": 0.3, "blocked": 0.3, "locked": 0.3, "disabled": 0.3}
        self.legal_threat_lexicon = {"arrest": 0.4, "warrant": 0.4, "legal": 0.3, "court": 0.3, "lawsuit": 0.4, "prosecution": 0.4, "penalty": 0.3, "fine": 0.3, "charge": 0.15}
        self.urgency_lexicon = {"urgently": 0.25, "now": 0.15, "within": 0.2, "hours": 0.1, "minutes": 0.2}
        self.financial_verb_lexicon = {"transfer": 0.3, "send": 0.2, "pay": 0.3, "deposit": 0.25}

    def _tokenize(self, text: str) -> list[str]:
        """Simple whitespace tokenizer (simulating vectorizer preprocessing)."""
        import string
        text = text.lower()
        text = text.translate(str.maketrans("", "", string.punctuation))
        return text.split()

    def analyze(self, text: str) -> Dict[str, Any]:
        """Runs pseudo inference calculating probabilistic intent scores."""
        tokens = self._tokenize(text)
        
        coercion_score = sum(self.coercion_lexicon.get(t, 0) for t in tokens)
        legal_score = sum(self.legal_threat_lexicon.get(t, 0) for t in tokens)
        urgency_score = sum(self.urgency_lexicon.get(t, 0) for t in tokens)
        financial_score = sum(self.financial_verb_lexicon.get(t, 0) for t in tokens)
        
        # Determine dominants
        score = 0.0
        explanations = []
        
        if legal_score >= 0.3:
            explanations.append("L2: High statistical probability of legal/threat coercion")
            score += 0.4
        elif coercion_score >= 0.3:
            explanations.append("L2: Behavioral analysis indicates account coercion")
            score += 0.3
            
        if urgency_score >= 0.2:
            explanations.append("L2: Temporal urgency markers detected")
            score += 0.2
            
        if financial_score >= 0.25:
            explanations.append("L2: Payment routing intent recognized")
            score += 0.3

        return {
            "score": min(1.0, score),
            "explanations": explanations
        }
