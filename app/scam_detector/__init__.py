"""Scam detection package.

Exports the main detect_scam function and ScamSignal dataclass.
"""

from app.scam_detector.scorer import ScamSignal, detect_scam

# Shim for testing - allows unittest.mock.patch("app.scam_detector.client") to work
client = None

__all__ = ["detect_scam", "ScamSignal"]
