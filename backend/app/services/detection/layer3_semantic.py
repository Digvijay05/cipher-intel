"""Layer 3: Deep Semantic Context (150-300ms latency).

The scalable tier utilizing Transformer-based classification (e.g. DistilRoBERTa).
Invoked primarily for breaking ties in ambiguous inputs. 
Provides a graceful fallback to a stub if models aren't available.
"""

import logging
from typing import Dict, Any
from app.config.settings import settings

logger = logging.getLogger(__name__)


class SemanticContextLayer:
    """Deep embedding semantic analysis for complex template matching."""

    def __init__(self):
        self.model_loaded = False
        self.pipeline = None
        self.max_latency_ms = 300

        # Optional ML pipeline loading based on feature flag
        if getattr(settings, "DETECTION_ML_ENABLED", False):
            try:
                from transformers import pipeline
                # We use a zero-shot classifier for easy plug-and-play without 
                # a highly coupled custom fine-tuned model for the initial refactor.
                logger.info("Initializing ML pipeline for Layer 3 semantic detection...")
                self.pipeline = pipeline("zero-shot-classification", model="facebook/bart-large-mnli")
                self.model_loaded = True
                logger.info("L3 Semantic ML Pipeline loaded successfully.")
            except ImportError:
                logger.warning("transformers library not found. L3 semantic classifier running in stub mode.")
            except Exception as e:
                logger.error(f"Failed to load ML pipeline: {e}")

    def analyze(self, text: str, historical_context: list[str] = None) -> Dict[str, Any]:
        """Run deep semantic analysis if enabled.
        
        Falls back to a keyword/cluster stub if pipeline is unavailable.
        """
        score = 0.0
        explanations = []

        if self.model_loaded and self.pipeline:
            try:
                candidate_labels = ["scam", "fraud", "phishing", "legitimate", "safe"]
                result = self.pipeline(text, candidate_labels)
                
                labels = result['labels']
                scores = result['scores']
                
                scam_score = 0.0
                for label, s in zip(labels, scores):
                    if label in ["scam", "fraud", "phishing"]:
                        scam_score += s
                        
                if scam_score >= 0.5:
                    score = min(1.0, scam_score * 1.2)  # Boost confidence slightly
                    explanations.append(f"L3: Semantic model classified as scam (confidence {score:.2f})")
                elif scam_score < 0.2:
                    score = 0.0
                else:
                    score = scam_score
                    
                if score > 0.4 and len(explanations) == 0:
                     explanations.append(f"L3: Semantic risk inferred ({score:.2f})")
                     
                return {"score": score, "explanations": explanations}
            except Exception as e:
                logger.error(f"ML evaluation failed: {e}")
                # Fallthrough to stub below if pipeline fails during inference

        # Feature placeholder fallback implementation:
        # P(Scam|L3) = Semantic similarity to known social engineering loops
        lower_text = text.lower()
        if "help me out" in lower_text and "gift card" in lower_text:
            score = 0.8
            explanations.append("L3: Semantic map closely aligns with 'Gift Card Request' phishing template")
            
        if "customs package" in lower_text and "held" in lower_text:
            score = 0.9
            explanations.append("L3: Matches 'Customs Delay / Advance Fee' semantic cluster")

        return {
            "score": min(1.0, score),
            "explanations": explanations
        }

