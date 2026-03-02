from fastapi import FastAPI
from app.api.routes import router
from app.engine.ml_model import DistilBERTScamClassifier
import logging
from contextlib import asynccontextmanager

logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(name)s - %(levelname)s - %(message)s')
logger = logging.getLogger(__name__)

@asynccontextmanager
async def lifespan(app: FastAPI):
    logger.info("Initializing ML models inside service lifespan...")
    # Pre-warm singleton load
    classifier = DistilBERTScamClassifier(model_path="../../cipher_distilbert_detection", threshold=0.0027917588595300913)
    if classifier.is_loaded():
        logger.info(f"DistilBERT initialized via {classifier.backend_type} backend.")
    yield
    logger.info("Shutting down detection service.")

app = FastAPI(title="CIPHER Detection Service", lifespan=lifspan)
app.include_router(router)
