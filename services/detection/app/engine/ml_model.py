import asyncio
import torch
from transformers import AutoTokenizer, AutoModelForSequenceClassification
import time

class DistilBERTScamClassifier:
    """Infrastructure layer for loading and running the DistilBERT model.
    Loads precisely once globally."""
    _instance = None

    def __new__(cls, model_path: str = "./cipher_distilbert_detection", threshold: float = 0.0027917588595300913):
        if cls._instance is None:
            cls._instance = super(DistilBERTScamClassifier, cls).__new__(cls)
            cls._instance._initialize(model_path, threshold)
        return cls._instance

    def _initialize(self, model_path: str, threshold: float):
        self.model_path = model_path
        self.threshold = threshold
        self.device = torch.device("cpu") # explicit requirement
        
        # Load ONNX or Torch (assuming Torch by default here based on standard export, can switch config)
        self.tokenizer = AutoTokenizer.from_pretrained(self.model_path)
        self.model = AutoModelForSequenceClassification.from_pretrained(self.model_path)
        self.model.to(self.device)
        self.model.eval()
        self.backend_type = "torch"
    
    def _predict_sync(self, text: str) -> dict:
        """Synchronous CPU inference function"""
        t0 = time.perf_counter()
        inputs = self.tokenizer(text, return_tensors="pt", padding=True, truncation=True, max_length=128)
        inputs = {k: v.to(self.device) for k, v in inputs.items()}
        
        with torch.inference_mode():
            outputs = self.model(**inputs)
            probs = torch.nn.functional.softmax(outputs.logits, dim=-1)[0].numpy()
            scam_prob = float(probs[1])
            
        latency_ms = (time.perf_counter() - t0) * 1000
        scam_detected = scam_prob >= self.threshold
        
        return {
            "scam_detected": scam_detected,
            "confidence": scam_prob,
            "threshold": self.threshold,
            "latency_ms": latency_ms,
            "text_length": len(text)
        }

    async def predict(self, text: str) -> dict:
        """Async-safe inference to prevent blocking the event loop."""
        return await asyncio.to_thread(self._predict_sync, text)

    def is_loaded(self) -> bool:
        return self.model is not None
