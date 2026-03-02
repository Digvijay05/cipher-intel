"""Scammer profiling service.

Maintains aggregate information on individual scammers across multiple
sessions. Consumes events from the event bus to autonomously update scores.
"""

import json
import logging
from datetime import datetime, timezone
from typing import Any, Dict, List, Optional

from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy.future import select

from app.db import AsyncSessionLocal
from app.models.db_models import ScammerProfile

logger = logging.getLogger(__name__)


def _merge_dict_lists(existing: Dict[str, List[Any]], new_data: Dict[str, List[Any]]) -> Dict[str, List[Any]]:
    """Merge two dictionaries containing lists, deduplicating elements."""
    merged = dict(existing)
    for key, value in new_data.items():
        if not isinstance(value, list):
            continue
        if key not in merged:
            merged[key] = []
        # Add new elements not already in existing
        for item in value:
            # Simple deduplication (won't work for dicts in lists, but ok for primitives)
            if item not in merged[key] and item != "":
                merged[key].append(item)
    return merged


class ScammerProfileService:
    """Manages scammer profiles. Typically used as a singleton."""

    async def get_by_sender(self, sender: str) -> Optional[ScammerProfile]:
        """Retrieve a profile by sender ID."""
        async with AsyncSessionLocal() as session:
            result = await session.execute(
                select(ScammerProfile).where(ScammerProfile.sender == sender)
            )
            return result.scalar_one_or_none()

    async def get_profiles(self, limit: int = 50, status: str = None) -> List[ScammerProfile]:
        """Get latest profiles."""
        async with AsyncSessionLocal() as session:
            query = select(ScammerProfile).order_by(ScammerProfile.last_seen.desc())
            if status:
                query = query.where(ScammerProfile.status == status)
            query = query.limit(limit)
            result = await session.execute(query)
            return list(result.scalars().all())

    async def handle_engagement_turn(self, payload: Dict[str, Any]) -> None:
        """Handle an engagement.turn event to update profile."""
        sender = payload.get("sender")
        if not sender or sender == "agent":
            return

        intel_buffer = payload.get("intel_buffer", {})
        
        async with AsyncSessionLocal() as db_session:
            try:
                result = await db_session.execute(
                    select(ScammerProfile).where(ScammerProfile.sender == sender)
                )
                profile = result.scalar_one_or_none()

                now = datetime.now(timezone.utc)

                if not profile:
                    profile = ScammerProfile(
                        sender=sender,
                        first_seen=now,
                        last_seen=now,
                        total_engagements=1,
                        total_turns=1,
                        risk_score=0.0,
                    )
                    db_session.add(profile)
                    logger.info(f"Created new profile for generic sender alias: {sender}")
                else:
                    profile.total_turns += 1
                    profile.last_seen = now

                # Extract and merge entities
                try:
                    ext_ent = profile.extracted_entities
                    if ext_ent is None or ext_ent == "":
                        existing_intel = {}
                    else:
                        existing_intel = json.loads(ext_ent)
                except json.JSONDecodeError:
                    existing_intel = {}

                merged_intel = _merge_dict_lists(existing_intel, intel_buffer)
                profile.extracted_entities = json.dumps(merged_intel)

                # Recompute naive risk score: +0.05 per entity found
                entity_score = sum(len(items) for items in merged_intel.values()) * 0.05
                # +0.01 per turn
                turn_score = profile.total_turns * 0.01
                
                profile.risk_score = min(1.0, entity_score + turn_score)

                await db_session.commit()
            except Exception as e:
                logger.error(f"Error handling profile update for {sender}: {e}", exc_info=True)
                await db_session.rollback()

    async def handle_scam_detected(self, payload: Dict[str, Any]) -> None:
        """Handle scam.detected event."""
        sender = payload.get("sender")
        if not sender or sender == "agent":
            return

        async with AsyncSessionLocal() as db_session:
            try:
                result = await db_session.execute(
                    select(ScammerProfile).where(ScammerProfile.sender == sender)
                )
                profile = result.scalar_one_or_none()
                now = datetime.now(timezone.utc)

                # If profile doesn't exist, this means scam detection triggered on first msg
                if not profile:
                    profile = ScammerProfile(
                        sender=sender,
                        first_seen=now,
                        last_seen=now,
                        total_engagements=1,
                        total_turns=0,
                        risk_score=payload.get("confidence_score", 0.0),
                    )
                    db_session.add(profile)
                else:
                    # Bump engagement count since they started a new scam attempt
                    # Assuming a new session triggered this.
                    # Simplistic approach: if last_seen > 1 hour ago, count as new engagement
                    time_diff = (now - profile.last_seen.replace(tzinfo=timezone.utc)).total_seconds()
                    if time_diff > 3600:
                        profile.total_engagements += 1
                    
                    profile.last_seen = now
                    profile.risk_score = max(profile.risk_score, payload.get("confidence_score", 0.0))

                await db_session.commit()
            except Exception as e:
                logger.error(f"Error handling scam.detected for {sender}: {e}")
                await db_session.rollback()


# Global service instance
_profile_service: Optional[ScammerProfileService] = None

def get_profile_service() -> ScammerProfileService:
    global _profile_service
    if _profile_service is None:
        _profile_service = ScammerProfileService()
    return _profile_service
