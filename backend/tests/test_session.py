"""Unit tests for session management."""

import os
import pytest

# Set env vars before imports (required for Settings validation)
os.environ.setdefault("CIPHER_API_KEY", "test-key")
os.environ.setdefault("OLLAMA_API_KEY", "test-key")
os.environ.setdefault("OPENAI_API_KEY", "test-key")

from app.services.session import SessionState, get_session_store, InMemorySessionStore


class TestSessionState:
    """Tests for SessionState model."""

    def test_default_values(self) -> None:
        """Test SessionState has correct defaults."""
        state = SessionState(session_id="test-123")
        assert state.session_id == "test-123"
        assert state.turn_number == 0
        assert state.scam_score == 0.0
        assert state.is_scam is False
        assert state.agent_active is False
        assert "bankAccounts" in state.intel_buffer
        assert "upiIds" in state.intel_buffer

    def test_touch_updates_timestamp(self) -> None:
        """Test touch() updates updated_at."""
        state = SessionState(session_id="test-123")
        old_updated = state.updated_at
        import time
        time.sleep(0.01)
        state.touch()
        assert state.updated_at > old_updated

    def test_serialization(self) -> None:
        """Test converting state to dict for storage."""
        state = SessionState(session_id="test-123", turn_number=8)
        data = state.model_dump()
        assert data["session_id"] == "test-123"
        assert data["turn_number"] == 8
        # The original test also checked deserialization, so we'll keep that part
        json_str = state.model_dump_json()
        restored = SessionState.model_validate_json(json_str)
        assert restored.session_id == state.session_id
        assert restored.turn_number == state.turn_number


class TestInMemorySessionStore:
    """Tests for InMemorySessionStore."""

    @pytest.fixture
    def store(self) -> InMemorySessionStore:
        """Create a fresh store for each test."""
        return InMemorySessionStore()

    @pytest.mark.asyncio
    async def test_save_and_get(self, store: InMemorySessionStore) -> None:
        """Test saving and retrieving a session."""
        state = SessionState(session_id="test-123", turn_number=3)
        await store.save(state)
        retrieved = await store.get("test-123")
        assert retrieved is not None
        assert retrieved.turn_number == 3

    @pytest.mark.asyncio
    async def test_get_nonexistent(self, store: InMemorySessionStore) -> None:
        """Test getting a nonexistent session returns None."""
        result = await store.get("nonexistent")
        assert result is None

    @pytest.mark.asyncio
    async def test_exists(self, store: InMemorySessionStore) -> None:
        """Test exists check."""
        state = SessionState(session_id="test-123")
        await store.save(state)
        assert await store.exists("test-123") is True
        assert await store.exists("nonexistent") is False

    @pytest.mark.asyncio
    async def test_delete(self, store: InMemorySessionStore) -> None:
        """Test deleting a session."""
        state = SessionState(session_id="test-123")
        await store.save(state)
        await store.delete("test-123")
        assert await store.exists("test-123") is False


class TestGetSessionStore:
    """Tests for get_session_store factory."""

    def test_returns_in_memory_by_default(self) -> None:
        """Test that in-memory store is returned when no REDIS_URL."""
        import app.services.session as session_module
        session_module._session_store = None

        store = get_session_store()
        assert isinstance(store, InMemorySessionStore)
