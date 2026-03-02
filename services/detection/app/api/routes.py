from fastapi import APIRouter
from pydantic import BaseModel
from typing import List, Dict, Any

from app.engine.ensemble import DetectionEnsemble
from app.engine.ml_model import DistilBERTScamClassifier

router = APIRouter()
ensemble = DetectionEnsemble()

class AnalyzeRequest(BaseModel):
    text: str

class DetectionSource(BaseModel):
    scam_detected: bool
    confidence: float

class SourcesDetails(BaseModel):
    ml: Dict[str, Any]
    rules: Dict[str, Any]

class AnalyzeResponse(BaseModel):
    scam_detected: bool
    confidence: float
    risk_level: str
    reasons: List[str]
    sources: SourcesDetails

class HealthResponse(BaseModel):
    status: str
    model_loaded: bool
    threshold: float
    backend: str

@router.post("/api/v1/detect", response_model=AnalyzeResponse)
async def analyze_message(req: AnalyzeRequest):
    """
    Stateless scam detection utilizing the detection ensemble.
    Delegates to the engine which aggregates Rule-based + ML predictions.
    """
    result = await ensemble.evaluate(req.text)
    return AnalyzeResponse(**result)


@router.get("/health", response_model=HealthResponse)
async def health_check():
    """
    Health check mapping ML Model wrapper status and backend info.
    """
    ml_classifier = DistilBERTScamClassifier()
    return HealthResponse(
        status="healthy",
        model_loaded=ml_classifier.is_loaded(),
        threshold=ml_classifier.threshold,
        backend=ml_classifier.backend_type
    )
