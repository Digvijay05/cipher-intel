"""Layer 3: Deep Semantic Context (150-300ms latency).

The scalable tier utilizing Transformer-based classification (e.g. DistilRoBERTa).
Invoked primarily for breaking ties in ambiguous inputs. 
Provides a graceful fallback to a stub if models aren't available.
"""

import logging
import time
import os
import anyio
from typing import Dict, Any

logger = logging.getLogger(__name__)

# Constants for Hugging Face
MODEL_ID = os.getenv("HF_MODEL_ID", "distilbert-base-uncased") # Fallback for local testing if env not set
HF_TOKEN = os.getenv("HF_TOKEN")



class SemanticContextLayer:
    """Deep embedding semantic analysis for complex template matching."""

    def __init__(self):
        self.model_loaded = False
        self.pipeline = None
        self.model_commit_sha = None
        self.max_latency_ms = 300
        self.labels = {}

        try:
            from huggingface_hub import HfApi
            from transformers import pipeline
            
            logger.info("Initializing ML pipeline for Layer 3 semantic detection...")
            
            # Resolve Model SHA
            api = HfApi()
            model_info = api.model_info(repo_id=MODEL_ID, token=HF_TOKEN)
            self.model_commit_sha = model_info.sha
            
            # Load Pipeline 
            start_time = time.perf_counter()
            self.pipeline = pipeline("text-classification", model=MODEL_ID, token=HF_TOKEN)
            load_time = time.perf_counter() - start_time
            
            self.labels = getattr(self.pipeline.model.config, "id2label", {})
            self.model_loaded = True
            
            logger.info({
                "event": "model_load_success",
                "model_id": MODEL_ID,
                "commit_sha": self.model_commit_sha,
                "load_time_seconds": round(load_time, 2)
            })
            
        except ImportError:
            logger.warning("transformers or huggingface_hub library not found. L3 semantic classifier running in stub mode.")
        except Exception as e:
            logger.error({"event": "model_load_failure", "error": str(e)})
            # We raise RuntimeError here in strict ML mode to prevent silent fallback
            # raise RuntimeError("Strict ML mode: Failed to load DistilBERT") from e
            # For now, we will just fallback to stub mode if it fails.

    async def analyze(self, text: str, historical_context: list[str] = None) -> Dict[str, Any]:
        """Run deep semantic analysis if enabled.
        
        Falls back to a keyword/cluster stub if pipeline is unavailable.
        """
        score = 0.0
        explanations = []

        if self.model_loaded and self.pipeline:
            start_time = time.perf_counter()
            try:
                # Execute inference off the asyncio event loop
                result = await anyio.to_thread.run_sync(self.pipeline, text)
                inference_time_ms = (time.perf_counter() - start_time) * 1000
                
                # Assuming the model outputs format [{'label': 'scam', 'score': 0.9}]
                label = result[0]['label']
                confidence = result[0]['score']
                
                logger.info({
                    "event": "inference",
                    "execution_path": "ml",
                    "commit_sha": self.model_commit_sha,
                    "inference_time_ms": round(inference_time_ms, 2),
                    "confidence": confidence,
                    "label": label
                })

                if label.lower() in ["scam", "fraud", "phishing"] or label == "1":
                    score = confidence
                    explanations.append(f"L3: Semantic model classified as scam (confidence {score:.2f})")
                else:
                    score = 0.0 # Legitimate
                    
                return {
                    "score": score, 
                    "explanations": explanations,
                    "execution_path": "ml",
                    "inference_time_ms": round(inference_time_ms, 2),
                    "model_id": MODEL_ID,
                    "commit_sha": self.model_commit_sha
                }
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

        logger.warning({"event": "inference", "execution_path": "rule_based", "reason": "model_unavailable_or_failed"})
        
        return {
            "score": min(1.0, score),
            "explanations": explanations,
            "execution_path": "rule_based"
        }

