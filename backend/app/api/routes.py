"""Routes for CIPHER Threat Intelligence API.

Delegates processing to AgentController for orchestration.
"""

from fastapi import APIRouter, Depends, Request

from app.services.agent import get_controller
from app.middleware.auth import verify_api_key
from app.models.schemas import HoneypotRequest, HoneypotResponse

router = APIRouter()


# -----------------------------
# Health Check (No Auth Required)
# -----------------------------
@router.get("/health")
async def health_check() -> dict:
    """Health check endpoint for container orchestration.

    No authentication required. Used by Docker HEALTHCHECK and load balancers.

    Returns:
        Status dict indicating the service is healthy.
    """
    return {"status": "healthy"}

# -----------------------------
# Main CIPHER API Endpoint
# -----------------------------
@router.post("/api/honeypot/message", response_model=HoneypotResponse)
async def honeypot(
    req: HoneypotRequest,
    _: str = Depends(verify_api_key),
) -> HoneypotResponse:
    """Process incoming message and generate CIPHER agent response.

    Args:
        req: The honeypot request containing message and history.

    Returns:
        HoneypotResponse with status and agent reply.
    """
    controller = get_controller()
    reply = await controller.process_message(
        session_id=req.sessionId,
        message=req.message,
        conversation_history=req.conversationHistory,
    )

    return HoneypotResponse(
        status="success",
        reply=reply,
    )


# -----------------------------------
# Root POST Endpoint (Compatibility)
# -----------------------------------
@router.post("/", response_model=HoneypotResponse)
async def root_honeypot(
    req: HoneypotRequest,
    _: str = Depends(verify_api_key),
) -> HoneypotResponse:
    """Process incoming message at root path for backward compatibility.

    Mirrors /api/honeypot/message for clients posting to root URL.

    Args:
        req: The honeypot request containing message and history.

    Returns:
        HoneypotResponse with status and agent reply.
    """
    controller = get_controller()
    reply = await controller.process_message(
        session_id=req.sessionId,
        message=req.message,
        conversation_history=req.conversationHistory,
    )

    return HoneypotResponse(
        status="success",
        reply=reply,
    )


# -----------------------------------
# CIPHER Reachability Test
# -----------------------------------
@router.api_route("/api/honeypot/test", methods=["GET", "POST"])
async def test_endpoint(
    request: Request,
    _: str = Depends(verify_api_key),
) -> dict:
    """Test endpoint to verify CIPHER service is reachable.

    Args:
        request: The incoming request.

    Returns:
        Status dict indicating endpoint is reachable.
    """
    # IMPORTANT:
    # - Do NOT read body
    # - Do NOT validate body
    # - Just respond OK
    return {
        "status": "ok",
        "message": "CIPHER endpoint reachable",
    }
