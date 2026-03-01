"""Session state and storage management.

Provides session state tracking, in-memory storage, and Redis-backed
storage for production use.
"""

import json
import logging
import time
from abc import ABC, abstractmethod
from typing import Any, Dict, List, Optional

from pydantic import BaseModel, Field

from app.config.settings import settings

logger = logging.getLogger(__name__)


# ---------------------------------------------------------------------------
# Session state model
# ---------------------------------------------------------------------------

class SessionState(BaseModel):
    """State tracked for each engagement session.

    Attributes:
        session_id: Unique identifier for the session.
        turn_number: Current turn number in engagement.
        state: Current session state in the state machine.
        scam_score: Cumulative scam confidence score.
        is_scam: Whether scam has been detected.
        agent_active: Whether the CIPHER agent is engaged.
        persona_id: Active persona identifier.
        intel_buffer: Extracted intelligence data.
        created_at: Timestamp when session was created.
        updated_at: Timestamp when session was last updated.
    """

    session_id: str
    turn_number: int = 0
    state: str = "idle"
    scam_score: float = 0.0
    is_scam: bool = False
    agent_active: bool = False
    persona_id: str = ""
    intel_buffer: Dict[str, Any] = Field(default_factory=lambda: {
        "bankAccounts": [],
        "upiIds": [],
        "phishingLinks": [],
        "phoneNumbers": [],
        "suspiciousKeywords": [],
    })
    created_at: float = Field(default_factory=time.time)
    updated_at: float = Field(default_factory=time.time)

    def touch(self) -> None:
        """Update the updated_at timestamp."""
        self.updated_at = time.time()

    def advance_state(
        self,
        scam_detected: bool,
        max_turns: int,
    ) -> None:
        """Advance session through the state machine.

        State transitions per CDB § 4:
            idle → detecting (on message received)
            detecting → engaging (scam confidence >= 0.50)
            detecting → safe (message is clean)
            engaging → engaging (turn++ < max)
            engaging → completing (turn >= max or disengage)
            completing → completed (after callback)

        Args:
            scam_detected: Whether scam was detected this turn.
            max_turns: Maximum allowed turns.
        """
        if self.state == "idle":
            self.state = "detecting"

        if self.state == "detecting":
            if scam_detected:
                self.state = "engaging"
                self.agent_active = True
            else:
                self.state = "safe"
            return

        if self.state == "engaging":
            if self.turn_number >= max_turns:
                self.state = "completing"
            return

        if self.state == "completing":
            self.state = "completed"
            self.agent_active = False
            return


# ---------------------------------------------------------------------------
# Abstract store interface
# ---------------------------------------------------------------------------

class SessionStore(ABC):
    """Abstract base class for session storage."""

    @abstractmethod
    async def get(self, session_id: str) -> Optional[SessionState]:
        """Retrieve a session by ID."""
        pass

    @abstractmethod
    async def save(self, state: SessionState) -> None:
        """Save or update a session."""
        pass

    @abstractmethod
    async def exists(self, session_id: str) -> bool:
        """Check if a session exists."""
        pass

    @abstractmethod
    async def delete(self, session_id: str) -> None:
        """Delete a session."""
        pass


# ---------------------------------------------------------------------------
# In-memory implementation
# ---------------------------------------------------------------------------

class InMemorySessionStore(SessionStore):
    """In-memory session store for local testing."""

    def __init__(self) -> None:
        self._store: Dict[str, SessionState] = {}

    async def get(self, session_id: str) -> Optional[SessionState]:
        """Retrieve a session by ID."""
        return self._store.get(session_id)

    async def save(self, state: SessionState) -> None:
        """Save or update a session."""
        state.touch()
        self._store[state.session_id] = state

    async def exists(self, session_id: str) -> bool:
        """Check if a session exists."""
        return session_id in self._store

    async def delete(self, session_id: str) -> None:
        """Delete a session."""
        self._store.pop(session_id, None)


# ---------------------------------------------------------------------------
# Redis implementation
# ---------------------------------------------------------------------------

class RedisSessionStore(SessionStore):
    """Redis-backed session store for production."""

    def __init__(self, redis_url: str) -> None:
        """Initialize Redis connection.

        Args:
            redis_url: Redis connection URL.
        """
        import redis.asyncio as redis

        self._client = redis.from_url(redis_url, decode_responses=True)
        self._prefix = settings.REDIS_KEY_PREFIX
        self._ttl = settings.REDIS_SESSION_TTL_SECONDS

    def _key(self, session_id: str) -> str:
        """Generate Redis key for session."""
        return f"{self._prefix}{session_id}"

    async def get(self, session_id: str) -> Optional[SessionState]:
        """Retrieve a session by ID."""
        try:
            data = await self._client.get(self._key(session_id))
            if data:
                return SessionState.model_validate_json(data)
            return None
        except Exception as e:
            logger.error(f"Redis get failed: {e}")
            return None

    async def save(self, state: SessionState) -> None:
        """Save or update a session."""
        try:
            state.touch()
            await self._client.setex(
                self._key(state.session_id),
                self._ttl,
                state.model_dump_json(),
            )
        except Exception as e:
            logger.error(f"Redis save failed: {e}")

    async def exists(self, session_id: str) -> bool:
        """Check if a session exists."""
        try:
            return bool(await self._client.exists(self._key(session_id)))
        except Exception as e:
            logger.error(f"Redis exists failed: {e}")
            return False

    async def delete(self, session_id: str) -> None:
        """Delete a session."""
        try:
            await self._client.delete(self._key(session_id))
        except Exception as e:
            logger.error(f"Redis delete failed: {e}")


# ---------------------------------------------------------------------------
# Factory
# ---------------------------------------------------------------------------

# Global store instance
_session_store: Optional[SessionStore] = None


def get_session_store() -> SessionStore:
    """Get or create the session store singleton.

    Uses Redis if REDIS_URL is set, otherwise falls back to in-memory.

    Returns:
        SessionStore instance.
    """
    global _session_store
    if _session_store is None:
        redis_url = settings.REDIS_URL
        if redis_url:
            logger.info("Using Redis session store")
            _session_store = RedisSessionStore(redis_url)
        else:
            logger.info("Using in-memory session store")
            _session_store = InMemorySessionStore()
    return _session_store
