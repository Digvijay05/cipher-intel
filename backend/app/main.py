"""FastAPI application entry point."""

from contextlib import asynccontextmanager
from typing import AsyncGenerator

from fastapi import FastAPI

from app.db import init_db_async
from app.logging import setup_logging
from app.api.routes import router
from app.api.websocket import router as ws_router, setup_websocket_subscriptions


@asynccontextmanager
async def lifespan(app: FastAPI) -> AsyncGenerator[None, None]:
    """Application lifespan handler for startup/shutdown."""
    # Startup: Configure logging first
    setup_logging()
    # Startup: Initialize database
    await init_db_async()
    
    # Startup: Initialize Event Bus Subscribers
    from app.services.events import get_event_bus
    from app.services.profiling import get_profile_service
    bus = get_event_bus()
    profiler = get_profile_service()
    await bus.subscribe("scam.detected", profiler.handle_scam_detected)
    await bus.subscribe("engagement.turn", profiler.handle_engagement_turn)
    
    # Initialize WebSocket Subscriptions
    setup_websocket_subscriptions()
    
    yield
    # Shutdown: Cleanup (if needed)


app = FastAPI(
    title="CIPHER — Threat Intelligence API",
    version="1.0.0",
    description="Conversational Intelligence Platform for Honeypot Engagement & Reporting.",
    lifespan=lifespan,
)

from fastapi.middleware.cors import CORSMiddleware

app.add_middleware(
    CORSMiddleware,
    allow_origins=["https://app.cipher.com", "http://localhost:3000"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(router)
app.include_router(ws_router)

