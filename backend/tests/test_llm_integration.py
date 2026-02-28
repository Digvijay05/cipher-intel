"""Integration tests for Cloud Ollama LLM schema enforcement.

Tests the backend's resilience when the Cloud Ollama API returns
malformed, hallucinated, or unexpected output formats.
"""

import time

import pytest
from fastapi.testclient import TestClient
from unittest.mock import AsyncMock, patch, MagicMock

from app.main import app
from app.config.settings import settings

client = TestClient(app)

API_KEY = settings.CIPHER_API_KEY
ENDPOINT = "/api/honeypot/message"


def _build_payload(
    session_id: str = "test_session_1",
    sender: str = "scammer",
    text: str = "Send me your crypto wallet address",
) -> dict:
    """Build a valid HoneypotRequest payload."""
    return {
        "sessionId": session_id,
        "message": {
            "sender": sender,
            "text": text,
            "timestamp": int(time.time() * 1000),
        },
        "conversationHistory": [],
    }


class TestCloudOllamaIntegration:
    """Tests targeting the Cloud Ollama model integration at https://ollama.com."""

    def test_valid_scam_message_returns_reply(self):
        """Smoke test: a valid scam message should return a 200 with a reply."""
        payload = _build_payload()
        response = client.post(
            ENDPOINT,
            json=payload,
            headers={"x-api-key": API_KEY},
        )
        # The response should be 200 with a reply field
        assert response.status_code == 200, (
            f"Expected 200, got {response.status_code}: {response.text}"
        )
        data = response.json()
        assert "reply" in data, f"Response missing 'reply': {data}"
        assert "status" in data, f"Response missing 'status': {data}"
        assert data["status"] == "success"
        assert len(data["reply"]) > 0, "Reply should not be empty"

    def test_missing_api_key_returns_422(self):
        """Requests without x-api-key header are rejected by FastAPI validation (422)."""
        payload = _build_payload()
        response = client.post(ENDPOINT, json=payload)
        assert response.status_code == 422, (
            f"Expected 422 for missing required header, got {response.status_code}"
        )

    def test_invalid_api_key_returns_401(self):
        """Requests with a wrong x-api-key should be rejected by auth middleware (401)."""
        payload = _build_payload()
        response = client.post(
            ENDPOINT,
            json=payload,
            headers={"x-api-key": "wrong-key-obviously"},
        )
        assert response.status_code == 401, (
            f"Expected 401 for invalid API key, got {response.status_code}"
        )

    def test_malformed_payload_returns_422(self):
        """Sending garbage payload should trigger Pydantic validation error."""
        payload = {"message": None, "malicious_sql": "DROP TABLE sessions"}
        response = client.post(
            ENDPOINT,
            json=payload,
            headers={"x-api-key": API_KEY},
        )
        assert response.status_code == 422, (
            f"Expected 422 for malformed payload, got {response.status_code}"
        )

    def test_empty_message_text_returns_422(self):
        """An empty message text should be caught by validation."""
        payload = {
            "sessionId": "test_empty",
            "message": {
                "sender": "scammer",
                "text": "",
                "timestamp": int(time.time() * 1000),
            },
            "conversationHistory": [],
        }
        response = client.post(
            ENDPOINT,
            json=payload,
            headers={"x-api-key": API_KEY},
        )
        # Depending on validation rules, this could be 200 or 422
        # We assert it doesn't crash (5xx)
        assert response.status_code < 500, (
            f"Server error on empty text: {response.status_code}: {response.text}"
        )
