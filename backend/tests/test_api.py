"""Integration tests for /api/v1/engage endpoint."""

import os
from unittest.mock import MagicMock, patch

import pytest
from fastapi.testclient import TestClient

# Set env vars before importing app (required for Settings validation)
os.environ.setdefault("CIPHER_API_KEY", "test-key")
os.environ.setdefault("OLLAMA_API_KEY", "test-key")
os.environ.setdefault("OPENAI_API_KEY", "test-key")

from app.main import app

client = TestClient(app)


class TestCipherMessageEndpoint:
    """Integration tests for POST /api/v1/engage."""

    @patch("app.api.routes.get_controller")
    def test_message_endpoint_returns_okay_for_non_scam(self, mock_get_controller: MagicMock) -> None:
        """Test that non-scam messages get dynamic benign response."""
        mock_controller = MagicMock()
        import asyncio
        future = asyncio.Future()
        future.set_result("Oh, hello dear. How are you?")
        mock_controller.process_message.return_value = future
        mock_get_controller.return_value = mock_controller

        response = client.post(
            "/api/v1/engage",
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
            "/api/v1/engage",
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
        assert data["status"] == "continue"
        assert data["reply"] == "I don't understand, how do I unblock it?"

    def test_auth_required(self) -> None:
        """Test that API key is required."""
        response = client.post(
            "/api/v1/engage",
            json={
                "sessionId": "test",
                "message": {"sender": "x", "text": "y", "timestamp": 0},
            },
        )
        assert response.status_code == 422  # Missing header

    def test_health_no_auth(self) -> None:
        """Test that health endpoint requires no auth."""
        response = client.get("/health")
        assert response.status_code == 200
        assert response.json()["status"] == "healthy"
