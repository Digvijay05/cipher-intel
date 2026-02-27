"""Integration tests for /api/honeypot/message endpoint."""

from unittest.mock import MagicMock, patch

import pytest
from fastapi.testclient import TestClient

# Patch config before importing app
with patch.dict(
    "os.environ",
    {"CIPHER_API_KEY": "test-key", "OPENAI_API_KEY": "test-openai-key"},
):
    from app.main import app

client = TestClient(app)


class TestHoneypotMessageEndpoint:
    """Integration tests for POST /api/honeypot/message."""

    @patch("app.api.routes.get_controller")
    def test_message_endpoint_returns_okay_for_non_scam(self, mock_get_controller: MagicMock) -> None:
        """Test that non-scam messages get dynamic benign response."""
        # Setup mock controller to return a dynamic response without hitting the LLM
        mock_controller = MagicMock()
        import asyncio
        future = asyncio.Future()
        future.set_result("Oh, hello dear. How are you?")
        mock_controller.process_message.return_value = future
        mock_get_controller.return_value = mock_controller
        
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
        assert data["reply"] == "Oh, hello dear. How are you?"

    @patch("app.api.routes.get_controller")
    def test_message_endpoint_returns_success(self, mock_get_controller: MagicMock) -> None:
        """Test that message endpoint returns success response."""
        mock_controller = MagicMock()
        import asyncio
        future = asyncio.Future()
        future.set_result("I don't understand, how do I unblock it?")
        mock_controller.process_message.return_value = future
        mock_get_controller.return_value = mock_controller

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
        assert data["reply"] == "I don't understand, how do I unblock it?"


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
