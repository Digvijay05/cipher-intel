"""Unit tests for the multi-layer scam detection engine."""

import os

# Set env vars before imports (required for Settings validation)
os.environ.setdefault("CIPHER_API_KEY", "test-key")
os.environ.setdefault("OLLAMA_API_KEY", "test-key")
os.environ.setdefault("OPENAI_API_KEY", "test-key")

import pytest

from app.services.detection import detect_scam, ScamSignal, ScamDetectorEngine
from app.services.detection.layer1_heuristics import HeuristicsLayer


class TestScamRules:
    """Test individual L1 rule matching via HeuristicsLayer."""

    def setup_method(self) -> None:
        self.layer = HeuristicsLayer()

    def test_upi_id_detection(self) -> None:
        """Test UPI ID pattern detection."""
        text = "Please send payment to scammer@ybl"
        result = self.layer.analyze(text)
        assert result["score"] >= 0.4
        assert any("UPI" in expl for expl in result["explanations"])

    def test_link_detection(self) -> None:
        """Test URL link detection."""
        text = "Click here: http://bit.ly/1234abcd"
        result = self.layer.analyze(text)
        assert result["score"] >= 0.35
        assert any("URL" in expl for expl in result["explanations"])


class TestMultiLayerScamScorer:
    """Test the ensemble engine and routing logic."""

    def test_scam_detected_above_threshold(self) -> None:
        """Test that high-signal messages trigger scam detection."""
        text = "Your account is blocked! Verify immediately with OTP. Send money to scam@ybl"
        result = detect_scam(text)

        assert getattr(result, "scamDetected") is True
        assert result.scamDetected is True
        assert result.confidenceScore >= 0.5
        assert result.riskLevel in ["medium", "high", "critical"]
        assert len(result.explanations) > 0

    def test_no_scam_below_threshold(self) -> None:
        """Test that low-signal messages don't trigger scam detection."""
        text = "Hello, can you help me with my order?"
        result = detect_scam(text)
        assert result.scamDetected is False
        assert result.confidenceScore < 0.5
        assert result.riskLevel == "low"

    def test_session_decay_memory(self) -> None:
        """Test that the engine tracks historical risk."""
        text = "Hello there"

        result_clean = detect_scam(text)
        assert result_clean.scamDetected is False

        result_decay = detect_scam(text, previous_session_score=0.95)
        assert result_decay.scamDetected is True
        assert result_decay.confidenceScore >= 0.57
        assert any("Session risk elevated" in expl for expl in result_decay.explanations)

    def test_confidence_is_capped(self) -> None:
        """Test that confidence never exceeds 1.0."""
        text = "URGENT! Account blocked! Legal action! Send OTP to scam@ybl immediately! Pay fine of Rs 5000!"
        result = detect_scam(text)
        assert result.confidenceScore <= 1.0
