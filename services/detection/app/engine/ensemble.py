from .ml_model import DistilBERTScamClassifier
from .rules import RuleEngine
import logging

class DetectionEnsemble:
    """Application layer orchestration that combines Rules and ML classifier."""
    
    def __init__(self):
        self.rule_engine = RuleEngine()
        self.ml_classifier = DistilBERTScamClassifier()
        
    async def evaluate(self, text: str) -> dict:
        """Run ML and Rules. Combine via OR logic."""
        
        # 1. Run ultra-fast rules
        rule_res = self.rule_engine.analyze(text)
        
        # 2. Run async fine-tuned ML model
        ml_res = await self.ml_classifier.predict(text)
        
        # 3. Aggregate -> OR logic (either high rule score or ML detected)
        is_scam = True if (ml_res["scam_detected"] or rule_res["scam_detected"]) else False
        
        # Base confidence is max of both
        final_confidence = max(rule_res["score"], ml_res["confidence"])
        
        reasons = []
        if ml_res["scam_detected"]:
            reasons.append(f"ML Model confidence: {ml_res['confidence']:.2f}")
        if rule_res["explanations"]:
            reasons.extend(rule_res["explanations"])
            
        logging.info(
            f"Ensemble Decision: Scam={is_scam} | ML={ml_res['scam_detected']} ({ml_res['confidence']:.2f}) | "
            f"Rules={rule_res['scam_detected']} ({rule_res['score']:.2f}) | Latency={ml_res.get('latency_ms', 0):.2f}ms"
        )
        
        return {
            "scam_detected": is_scam,
            "confidence": final_confidence,
            "risk_level": self._map_risk_level(final_confidence),
            "sources": {
                "ml": ml_res,
                "rules": rule_res
            },
            "reasons": reasons
        }
        
    def _map_risk_level(self, confidence: float) -> str:
        """Map raw float confidence into standard risk tier."""
        if confidence >= 0.85: return "critical"
        if confidence >= 0.65: return "high"
        if confidence >= 0.45: return "medium"
        return "low"
