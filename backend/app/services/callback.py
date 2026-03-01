"""Callback service for system invariants.

Responsible for delivering intelligence to the configured callback URL
when a scam engagement has successfully concluded. Includes retry logic
and delivery tracking.
"""

import asyncio
import logging
from datetime import datetime, timezone
import httpx

from app.config.settings import settings
from app.services.session import SessionState

logger = logging.getLogger(__name__)


async def fire_final_callback(session: SessionState) -> bool:
    """
    SYSTEM INVARIANT: Fire callback when engagement completes with scam detected.
    
    Args:
        session: The completed session state
        
    Returns:
        bool: True if delivery successful, False otherwise.
    """
    url = settings.CIPHER_CALLBACK_URL
    if not url:
        logger.warning(f"[Session {session.session_id}] CIPHER_CALLBACK_URL not configured — skipping callback")
        return False
    
    if not session.is_scam:
        logger.info(f"[Session {session.session_id}] Scam not detected — callback not required")
        return False
        
    if session.state != "completed":
        logger.warning(f"[Session {session.session_id}] Session not in 'completed' state — skipping callback")
        return False
    
    payload = {
        "session_id": session.session_id,
        "scam_detected": True,
        "confidence_score": session.scam_score,
        "intelligence": session.intel_buffer,
        "turn_count": session.turn_number,
        "completed_at": datetime.now(timezone.utc).isoformat(),
    }
    
    # Retry with exponential backoff
    for attempt in range(settings.LLM_MAX_RETRIES):
        try:
            async with httpx.AsyncClient(timeout=10.0) as client:
                logger.info(f"[Session {session.session_id}] Firing callback to {url} (attempt {attempt+1})")
                resp = await client.post(url, json=payload)
                resp.raise_for_status()
                logger.info(f"[Session {session.session_id}] Callback delivered successfully")
                return True
        except Exception as e:
            logger.error(f"[Session {session.session_id}] Callback attempt {attempt+1} failed: {e}")
            await asyncio.sleep(settings.LLM_RETRY_DELAY_SECONDS * (2 ** attempt))
    
    logger.critical(f"INVARIANT VIOLATION: All callback attempts failed for session {session.session_id}")
    return False
