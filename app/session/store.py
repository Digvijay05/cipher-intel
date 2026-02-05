"""Session storage implementations.

Provides both in-memory and Redis-backed session stores.
"""

import json
import logging
from abc import ABC, abstractmethod
from typing import Dict, Optional

from app.session.models import SessionState

logger = logging.getLogger(__name__)


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


class RedisSessionStore(SessionStore):
    """Redis-backed session store for production."""

    def __init__(self, redis_url: str, prefix: str = "honeypot:session:") -> None:
        """Initialize Redis connection.

        Args:
            redis_url: Redis connection URL.
            prefix: Key prefix for session keys.
        """
        import redis.asyncio as redis

        self._client = redis.from_url(redis_url, decode_responses=True)
        self._prefix = prefix
        self._ttl = 3600  # 1 hour TTL for sessions

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
        import os

        redis_url = os.getenv("REDIS_URL")
        if redis_url:
            logger.info("Using Redis session store")
            _session_store = RedisSessionStore(redis_url)
        else:
            logger.info("Using in-memory session store")
            _session_store = InMemorySessionStore()
    return _session_store
