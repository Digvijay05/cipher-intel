"""Routes for CIPHER Threat Intelligence API.

Delegates processing to AgentController for orchestration.
"""

from fastapi import APIRouter, Depends

from app.services.agent import get_controller
from app.middleware.auth import verify_api_key
from app.models.schemas import CipherRequest, EngageResponse, SessionStatus, EngageStatus, ScammerProfileResponse, ProfileListResponse
from pydantic import BaseModel
import json

router = APIRouter()

class FeatureFlagsResponse(BaseModel):
    engagement_enabled: bool
    kill_switch: bool

@router.get("/api/v1/feature-flags", response_model=FeatureFlagsResponse)
async def get_feature_flags():
    """
    Lightweight endpoint to broadcast the system's operational state to edge devices.
    Does not require DB access.
    """
    return FeatureFlagsResponse(
        engagement_enabled=True,
        kill_switch=False
    )


@router.get("/health")
async def health_check() -> dict:
    """Health check endpoint for container orchestration.

    No authentication required. Used by Docker HEALTHCHECK and load balancers.

    Returns:
        Status dict indicating the service is healthy.
    """
    return {"status": "healthy"}

from pydantic import BaseModel

class ThreatAnalysisResponse(BaseModel):
    confidence_score: float
    risk_level: str
    scam_detected: bool

@router.post("/api/v1/analyze", response_model=ThreatAnalysisResponse)
async def analyze_message(
    req: CipherRequest,
    _: str = Depends(verify_api_key),
) -> ThreatAnalysisResponse:
    """Stateless scam detection for client-side risk assessment."""
    from app.services.detection import detect_scam
    
    signal = detect_scam(req.message.text)
    
    return ThreatAnalysisResponse(
        confidence_score=signal.confidenceScore,
        risk_level=signal.riskLevel,
        scam_detected=signal.scamDetected
    )


@router.post("/api/v1/engage", response_model=EngageResponse)
async def engage_cipher(
    req: CipherRequest,
    _: str = Depends(verify_api_key),
) -> EngageResponse:
    """Process incoming message and generate CIPHER agent response.

    Args:
        req: The CIPHER request containing message and history.

    Returns:
        EngageResponse with status and agent reply.
    """
    controller = get_controller()
    reply = await controller.process_message(
        session_id=req.sessionId,
        message=req.message,
        conversation_history=req.conversationHistory,
    )

    # In a real implementation we would fetch the updated session state,
    # but for now we construct the response based on the controller output.
    # The actual state is stored inside the controller's session store.
    from app.services.session import get_session_store
    store = get_session_store()
    session = await store.get(req.sessionId)
    
    # Fallback values if session doesn't exist
    state = SessionStatus.COMPLETED
    turn = 0
    scam_detected = False
    confidence = 0.0
    
    if session:
        state = SessionStatus(session.state)
        turn = session.turn_number
        scam_detected = session.is_scam
        confidence = session.scam_score

    from app.models.schemas import EngageStatus
    
    status = EngageStatus.CONTINUE
    if not session:
        # Check if disabled
        if reply == "System currently unavailable.":
             status = EngageStatus.DISABLED
    elif state == SessionStatus.COMPLETED or state == SessionStatus.SAFE:
        status = EngageStatus.COMPLETED

    return EngageResponse(
        status=status,
        reply=reply,
        session_state=state,
        turn_number=turn,
        scam_detected=scam_detected,
        confidence_score=confidence,
    )

@router.get("/api/v1/engage/{session_id}")
async def get_engagement(
    session_id: str,
    _: str = Depends(verify_api_key),
) -> dict:
    """Retrieve session state for an engagement."""
    from app.services.session import get_session_store
    store = get_session_store()
    session = await store.get(session_id)
    if not session:
        from fastapi import HTTPException
        raise HTTPException(status_code=404, detail="Session not found")
    return session.model_dump()

@router.get("/api/v1/profile/{sender}", response_model=ScammerProfileResponse)
async def get_profile(
    sender: str,
    _: str = Depends(verify_api_key),
) -> ScammerProfileResponse:
    """Retrieve scammer profile by sender ID."""
    from app.services.profiling import get_profile_service
    from fastapi import HTTPException
    
    profile = await get_profile_service().get_by_sender(sender)
    if not profile:
        raise HTTPException(status_code=404, detail="Profile not found")
        
    entities = {}
    tactics, categories = [], []
    try:
        entities = json.loads(profile.extracted_entities)
        tactics = json.loads(profile.tactics_observed)
        categories = json.loads(profile.scam_categories)
    except json.JSONDecodeError:
        pass
        
    return ScammerProfileResponse(
        sender=profile.sender,
        first_seen=profile.first_seen.isoformat() if profile.first_seen else None,
        last_seen=profile.last_seen.isoformat() if profile.last_seen else None,
        total_engagements=profile.total_engagements,
        total_turns=profile.total_turns,
        risk_score=profile.risk_score,
        scam_categories=categories,
        extracted_entities=entities,
        tactics_observed=tactics,
        status=profile.status,
    )

@router.get("/api/v1/profiles", response_model=ProfileListResponse)
async def list_profiles(
    limit: int = 50,
    status: str = None,
    _: str = Depends(verify_api_key),
) -> ProfileListResponse:
    """List recent scammer profiles."""
    from app.services.profiling import get_profile_service
    
    profiles = await get_profile_service().get_profiles(limit=limit, status=status)
    
    response_list = []
    for profile in profiles:
        entities = {}
        tactics, categories = [], []
        try:
            entities = json.loads(profile.extracted_entities)
            tactics = json.loads(profile.tactics_observed)
            categories = json.loads(profile.scam_categories)
        except json.JSONDecodeError:
            pass
            
        response_list.append(ScammerProfileResponse(
            sender=profile.sender,
            first_seen=profile.first_seen.isoformat() if profile.first_seen else None,
            last_seen=profile.last_seen.isoformat() if profile.last_seen else None,
            total_engagements=profile.total_engagements,
            total_turns=profile.total_turns,
            risk_score=profile.risk_score,
            scam_categories=categories,
            extracted_entities=entities,
            tactics_observed=tactics,
            status=profile.status,
        ))
        
    return ProfileListResponse(profiles=response_list, total_count=len(response_list))

