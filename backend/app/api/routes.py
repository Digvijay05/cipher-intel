"""Routes for CIPHER Threat Intelligence API.

Delegates processing to AgentController for orchestration.
"""

from fastapi import APIRouter, Depends

from app.services.agent import get_controller
from app.middleware.auth import verify_api_key
from app.models.schemas import HoneypotRequest, HoneypotResponse

router = APIRouter()


@router.get("/health")
async def health_check() -> dict:
    """Health check endpoint for container orchestration.

    No authentication required. Used by Docker HEALTHCHECK and load balancers.

    Returns:
        Status dict indicating the service is healthy.
    """
    return {"status": "healthy"}


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
