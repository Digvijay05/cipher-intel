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
        self.model_commit_sha = None
        self.max_latency_ms = 800
        self.labels = {}
        
        # Check if we should attempt to use hugging face inference API
        if not HF_TOKEN:
            logger.warning("HF_TOKEN missing. L3 semantic classifier running in stub mode.")
            return

        try:
            from huggingface_hub import HfApi
            
            logger.info("Initializing Hugging Face Serverless Inference API connection...")
            
            # Resolve Model SHA via API to verify it's reachable and check the commit
            api = HfApi()
            model_info = api.model_info(repo_id=MODEL_ID, token=HF_TOKEN)
            self.model_commit_sha = model_info.sha
            
            # Use predefined mappings for the Inference API since we can't read the dynamic config object offline
            # We assume binary classification mappings standard to the honeypot
            self.labels = {0: "safe", 1: "scam"}
            self.model_loaded = True
            
            logger.info({
                "event": "model_load_success",
                "model_id": MODEL_ID,
                "commit_sha": self.model_commit_sha,
                "load_type": "serverless_inference_api"
            })
            
        except Exception as e:
            logger.error({"event": "model_load_failure", "error": str(e)})
            # Fallback to stub mode if the API check fails on startup.

    async def analyze(self, text: str, historical_context: list[str] = None) -> Dict[str, Any]:
        """Run deep semantic analysis if enabled.
        
        Falls back to a keyword/cluster stub if pipeline is unavailable.
        """
        score = 0.0
        explanations = []

        if self.model_loaded:
            start_time = time.perf_counter()
            try:
                import httpx
                
                # API Endpoint for hugging face models
                api_url = f"https://api-inference.huggingface.co/models/{MODEL_ID}"
                headers = {"Authorization": f"Bearer {HF_TOKEN}"}
                payload = {"inputs": text}
                
                # Execute inference call to hugging face hub
                async with httpx.AsyncClient() as client:
                    response = await client.post(api_url, headers=headers, json=payload, timeout=5.0)
                    response.raise_for_status()
                    
                result = response.json()
                inference_time_ms = (time.perf_counter() - start_time) * 1000
                
                # The HF Inference API returns a list of lists: [[{'label': 'LABEL_1', 'score': 0.9}]]
                top_prediction = result[0][0] if isinstance(result, list) and isinstance(result[0], list) else result[0]
                
                label = top_prediction['label']
                confidence = top_prediction['score']
                
                logger.info({
                    "event": "inference",
                    "execution_path": "ml_api",
                    "commit_sha": self.model_commit_sha,
                    "inference_time_ms": round(inference_time_ms, 2),
                    "confidence": confidence,
                    "label": label
                })

                # Determine scam mapping
                if label.lower() in ["scam", "fraud", "phishing", "label_1"] or label == "1":
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
            except httpx.TimeoutException:
                logger.warning({"event": "inference_timeout", "reason": "hf_inference_api_timeout"})
                # Fallthrough to stub
            except Exception as e:
                logger.error(f"ML evaluation failed via API: {e}")
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

