"""WebSocket endpoints for real-time dashboard streaming.

Connects to the event bus and pushes real-time detection, engagement, and 
completion events to connected clients.
"""

import asyncio
import json
import logging
from typing import Dict, Any, List

from fastapi import APIRouter, WebSocket, WebSocketDisconnect

from app.services.events import get_event_bus
from app.config.settings import settings

logger = logging.getLogger(__name__)

router = APIRouter()

class ConnectionManager:
    def __init__(self):
        self.active_connections: List[WebSocket] = []

    async def connect(self, websocket: WebSocket):
        await websocket.accept()
        self.active_connections.append(websocket)
        logger.info(f"Client connected. Total clients: {len(self.active_connections)}")

    def disconnect(self, websocket: WebSocket):
        self.active_connections.remove(websocket)
        logger.info(f"Client disconnected. Total clients: {len(self.active_connections)}")

    async def broadcast_json(self, message: Dict[str, Any]):
        disconnected = []
        for connection in self.active_connections:
            try:
                await connection.send_json(message)
            except RuntimeError:
                disconnected.append(connection)
            except Exception as e:
                logger.error(f"Failed to send to client: {e}")
                disconnected.append(connection)
                
        for broken in disconnected:
            try:
                self.disconnect(broken)
            except ValueError:
                pass


manager = ConnectionManager()


async def handle_scam_detected(payload: Dict[str, Any]):
    await manager.broadcast_json({"event": "scam.detected", "payload": payload})


async def handle_engagement_turn(payload: Dict[str, Any]):
    await manager.broadcast_json({"event": "engagement.turn", "payload": payload})


async def handle_engagement_completed(payload: Dict[str, Any]):
    await manager.broadcast_json({"event": "engagement.completed", "payload": payload})


# We need to register these to the event bus
def setup_websocket_subscriptions():
    bus = get_event_bus()
    asyncio.create_task(bus.subscribe("scam.detected", handle_scam_detected))
    asyncio.create_task(bus.subscribe("engagement.turn", handle_engagement_turn))
    asyncio.create_task(bus.subscribe("engagement.completed", handle_engagement_completed))
    logger.info("WebSocket manager subscribed to event bus.")


@router.websocket("/ws/live")
async def websocket_endpoint(websocket: WebSocket):
    """
    WebSocket endpoint for live dashboard.
    Streams events as they happen from the agent interactions.
    """
    if not getattr(settings, "FEATURE_LIVE_DASHBOARD", True):
        await websocket.close(code=1008, reason="Live dashboard disabled")
        return

    await manager.connect(websocket)
    try:
        while True:
            # We don't really expect to receive data, but we keep the connection open
            # and respond to pings/keepalives if the client sends them.
            data = await websocket.receive_text()
            if data == "ping":
                await websocket.send_text("pong")
    except WebSocketDisconnect:
        manager.disconnect(websocket)
    except Exception as e:
        logger.error(f"WebSocket error: {e}")
        manager.disconnect(websocket)
