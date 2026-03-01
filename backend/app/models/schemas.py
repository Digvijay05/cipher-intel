"""Pydantic models for API request/response schemas.

Defines the JSON contract for all CIPHER API endpoints.
"""

from enum import Enum
from typing import Any, Dict, List, Optional

from pydantic import BaseModel, Field


class Message(BaseModel):
    """Individual message in a conversation."""

    sender: str = Field(..., description="The sender of the message", examples=["scammer", "agent"])
    text: str = Field(..., description="The message content", examples=["Your account is blocked!"])
    timestamp: int = Field(..., description="Epoch milliseconds timestamp", examples=[1770005528731])


class Metadata(BaseModel):
    """Optional metadata for the request."""

    channel: Optional[str] = Field(None, description="Communication channel", examples=["sms", "whatsapp", "email"])
    language: Optional[str] = Field(None, description="Language code", examples=["en", "hi"])
    locale: Optional[str] = Field(None, description="Locale code", examples=["en-US", "en-IN"])


class CipherRequest(BaseModel):
    """Request payload for CIPHER message endpoint."""

    sessionId: str = Field(
        ...,
        description="Unique session identifier",
        examples=["sess-abc123-def456"],
    )
    message: Message = Field(..., description="The incoming message to process")
    conversationHistory: List[Message] = Field(
        default=[],
        description="Previous messages in the conversation",
    )
    metadata: Optional[Metadata] = Field(None, description="Optional request metadata")

    model_config = {
        "json_schema_extra": {
            "examples": [
                {
                    "sessionId": "sess-abc123-def456",
                    "message": {
                        "sender": "scammer",
                        "text": "Your bank account has been suspended. Verify immediately!",
                        "timestamp": 1770005528731,
                    },
                    "conversationHistory": [],
                    "metadata": {"channel": "sms", "language": "en", "locale": "en-US"},
                }
            ]
        }
    }


# Backward-compatible alias
HoneypotRequest = CipherRequest


class SessionStatus(str, Enum):
    """Session state machine states per CDB § 4."""

    IDLE = "idle"
    DETECTING = "detecting"
    ENGAGING = "engaging"
    COMPLETING = "completing"
    COMPLETED = "completed"
    SAFE = "safe"


class EngageStatus(str, Enum):
    """Engagement turn response status."""

    CONTINUE = "continue"
    COMPLETED = "completed"
    ERROR = "error"
    DISABLED = "disabled"


class CipherResponse(BaseModel):
    """Response payload for CIPHER message endpoint."""

    status: str = Field(..., description="Response status", examples=["success", "error"])
    reply: str = Field(
        ...,
        description="The CIPHER agent's reply",
        examples=["Oh dear, what should I do? Can you explain more slowly?"],
    )

    model_config = {
        "json_schema_extra": {
            "examples": [
                {
                    "status": "success",
                    "reply": "Oh my, that sounds serious! What do I need to do?",
                }
            ]
        }
    }


# Backward-compatible alias
HoneypotResponse = CipherResponse


class EngageResponse(BaseModel):
    """Response payload for /api/v1/engage per CDB § 4."""

    status: EngageStatus = Field(
        ..., description="Engagement turn status",
    )
    reply: Optional[str] = Field(
        None, description="Agent reply text (null when completed/disabled)",
    )
    session_state: SessionStatus = Field(
        ..., description="Current session state",
    )
    turn_number: int = Field(
        ..., description="Current turn number in engagement",
    )
    scam_detected: bool = Field(
        ..., description="Whether scam was detected in this session",
    )
    confidence_score: float = Field(
        ..., description="Current confidence score",
    )


class ScammerProfileResponse(BaseModel):
    """Response payload for /api/v1/profile/{sender}."""

    sender: str
    first_seen: Optional[str] = None
    last_seen: Optional[str] = None
    total_engagements: int = 0
    total_turns: int = 0
    risk_score: float = 0.0
    scam_categories: List[str] = Field(default_factory=list)
    extracted_entities: Dict[str, Any] = Field(default_factory=dict)
    tactics_observed: List[str] = Field(default_factory=list)
    status: str = "active"


class ProfileListResponse(BaseModel):
    """Response payload for /api/v1/profiles."""

    profiles: List[ScammerProfileResponse]
    total_count: int
