"""Session state models for tracking conversation progress."""

import time
from typing import Any, Dict, List

from pydantic import BaseModel, Field


class SessionState(BaseModel):
    """State tracked for each session.

    Attributes:
        session_id: Unique identifier for the session.
        message_count: Number of messages processed in this session.
        scam_score: Cumulative scam confidence score.
        is_scam: Whether scam has been detected.
        agent_active: Whether the honeypot agent is engaged.
        intel_buffer: Extracted intelligence data.
        callback_sent: Whether the final callback has been sent.
        created_at: Timestamp when session was created.
        updated_at: Timestamp when session was last updated.
    """

    session_id: str
    message_count: int = 0
    scam_score: float = 0.0
    is_scam: bool = False
    agent_active: bool = False
    intel_buffer: Dict[str, Any] = Field(default_factory=lambda: {
        "bankAccounts": [],
        "upiIds": [],
        "phishingLinks": [],
        "phoneNumbers": [],
        "suspiciousKeywords": [],
    })
    callback_sent: bool = False
    created_at: float = Field(default_factory=time.time)
    updated_at: float = Field(default_factory=time.time)

    def touch(self) -> None:
        """Update the updated_at timestamp."""
        self.updated_at = time.time()
