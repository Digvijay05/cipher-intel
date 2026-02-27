"""Integration tests for /api/honeypot/message endpoint."""

from unittest.mock import MagicMock, patch

import pytest
from fastapi.testclient import TestClient

# Patch config before importing app
with patch.dict(
    "os.environ",
    {"HONEYPOT_API_KEY": "test-key", "OPENAI_API_KEY": "test-openai-key"},
):
    from app.main import app

client = TestClient(app)


class TestHoneypotMessageEndpoint:
    """Integration tests for POST /api/honeypot/message."""

    def test_message_endpoint_returns_success(self) -> None:
        """Test that message endpoint returns success response."""
        response = client.post(
            "/api/honeypot/message",
            json={
                "sessionId": "test-session-123",
                "message": {
                    "sender": "scammer",
                    "text": "Your account is blocked! Verify now!",
                    "timestamp": 1770005528731,
                },
                "conversationHistory": [],
                "metadata": {"channel": "sms", "language": "en", "locale": "en-US"},
            },
            headers={"x-api-key": "test-key"},
        )

        assert response.status_code == 200
        data = response.json()
        assert data["status"] == "success"
        assert "reply" in data

    def test_message_endpoint_requires_auth(self) -> None:
        """Test that message endpoint requires API key."""
        response = client.post(
            "/api/honeypot/message",
            json={
                "sessionId": "test-session",
                "message": {"sender": "test", "text": "hello", "timestamp": "now"},
            },
        )

        assert response.status_code == 422  # Missing header

    def test_message_endpoint_rejects_invalid_api_key(self) -> None:
        """Test that message endpoint rejects invalid API key."""
        response = client.post(
            "/api/honeypot/message",
            json={
                "sessionId": "test-session",
                "message": {"sender": "test", "text": "hello", "timestamp": "now"},
            },
            headers={"x-api-key": "wrong-key"},
        )

        assert response.status_code == 401

    def test_message_endpoint_returns_okay_for_non_scam(self) -> None:
        """Test that non-scam messages get default response."""
        response = client.post(
            "/api/honeypot/message",
            json={
                "sessionId": "test-session-nonscam",
                "message": {
                    "sender": "friend",
                    "text": "Hey, how are you?",
                    "timestamp": 1770005528731,
                },
            },
            headers={"x-api-key": "test-key"},
        )

        assert response.status_code == 200
        data = response.json()
        assert data["reply"] == "Okay."


class TestTestEndpoint:
    """Integration tests for /api/honeypot/test."""

    def test_test_endpoint_get(self) -> None:
        """Test GET /api/honeypot/test returns ok."""
        response = client.get(
            "/api/honeypot/test",
            headers={"x-api-key": "test-key"},
        )

        assert response.status_code == 200
        data = response.json()
        assert data["status"] == "ok"
        assert "reachable" in data["message"].lower()

    def test_test_endpoint_post(self) -> None:
        """Test POST /api/honeypot/test returns ok."""
        response = client.post(
            "/api/honeypot/test",
            headers={"x-api-key": "test-key"},
        )

        assert response.status_code == 200
        data = response.json()
        assert data["status"] == "ok"

    def test_test_endpoint_requires_auth(self) -> None:
        """Test that test endpoint requires API key."""
        response = client.get("/api/honeypot/test")

        assert response.status_code == 422  # Missing header
