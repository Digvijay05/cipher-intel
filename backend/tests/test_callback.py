"""Unit tests for the Callback dispatch invariants."""

import pytest
import asyncio
from unittest.mock import patch, MagicMock, AsyncMock
from app.services.callback import fire_final_callback
from app.services.session import SessionState

@pytest.mark.asyncio
async def test_fire_callback_scam_detected():
    """Test callback fires when scam is detected and session is completed."""
    session = SessionState(session_id="123", persona_id="sender123")
    session.is_scam = True
    session.state = "completed"
    session.intel_buffer = {"bankAccounts": ["123456789"]}
    
    with patch("app.services.callback.settings.CIPHER_CALLBACK_URL", "http://test-callback.local"), \
         patch("app.services.callback.httpx.AsyncClient.post", new_callable=AsyncMock) as mock_post:
        
        mock_response = MagicMock()
        mock_response.raise_for_status.return_value = None
        mock_post.return_value = mock_response
        
        result = await fire_final_callback(session)
        assert result is True
        mock_post.assert_called_once()
        args, kwargs = mock_post.call_args
        assert "json" in kwargs
        payload = kwargs["json"]
        assert payload["scam_detected"] is True
        assert payload["intelligence"]["bankAccounts"] == ["123456789"]

@pytest.mark.asyncio
async def test_fire_callback_not_scam():
    """Test callback does NOT fire if no scam detected."""
    session = SessionState(session_id="123", persona_id="sender123")
    session.is_scam = False
    session.state = "completed"
    
    with patch("app.services.callback.settings.CIPHER_CALLBACK_URL", "http://test-callback.local"), \
         patch("app.services.callback.httpx.AsyncClient.post", new_callable=AsyncMock) as mock_post:
        
        result = await fire_final_callback(session)
        assert result is False
        mock_post.assert_not_called()

@pytest.mark.asyncio
async def test_fire_callback_no_url():
    """Test callback fails gracefully if no URL is configured."""
    session = SessionState(session_id="123", persona_id="sender123")
    session.is_scam = True
    session.state = "completed"
    
    with patch("app.services.callback.settings.CIPHER_CALLBACK_URL", None), \
         patch("app.services.callback.httpx.AsyncClient.post", new_callable=AsyncMock) as mock_post:
        
        result = await fire_final_callback(session)
        assert result is False
        mock_post.assert_not_called()
