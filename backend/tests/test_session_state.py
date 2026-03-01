"""Unit tests for the Session State Machine."""

import pytest
from app.services.session import SessionState

def test_session_initial_state():
    """Test that a new session starts in 'idle' state."""
    session = SessionState(session_id="123", persona_id="sender123")
    assert session.state == "idle"
    assert session.turn_number == 0

def test_session_advance_state_detecting():
    """Test transitions into engaging or safe via detecting."""
    session = SessionState(session_id="123", persona_id="sender123")
    
    # Scam detected -> engaging
    session.advance_state(scam_detected=True, max_turns=5)
    assert session.state == "engaging"
    
    # Safe message -> safe
    session2 = SessionState(session_id="456", persona_id="sender456")
    session2.advance_state(scam_detected=False, max_turns=5)
    assert session2.state == "safe"

def test_session_advance_state_engaging():
    """Test transitions while engaging."""
    session = SessionState(session_id="123", persona_id="sender123")
    session.state = "engaging"
    
    # Turn < MAX_TURNS
    session.turn_number = 3
    session.advance_state(scam_detected=True, max_turns=5)
    assert session.state == "engaging"
    
    # Turn >= MAX_TURNS
    session.turn_number = 5
    session.advance_state(scam_detected=True, max_turns=5)
    assert session.state == "completing"

def test_session_advance_state_completing():
    """Test transition from completing to completed."""
    session = SessionState(session_id="123", persona_id="sender123")
    session.state = "completing"
    session.advance_state(scam_detected=True, max_turns=5)
    assert session.state == "completed"

def test_session_advance_state_terminal():
    """Test that terminal states do not advance further."""
    session = SessionState(session_id="123", persona_id="sender123")
    session.state = "completed"
    session.advance_state(scam_detected=True, max_turns=5)
    assert session.state == "completed"
