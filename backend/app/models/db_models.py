"""Database models for persistence.

Defines SQLAlchemy ORM models for sessions, messages, scam analyses,
and extracted entities.
"""

import uuid
from datetime import datetime, timezone
from typing import List

from sqlalchemy import BigInteger, Boolean, DateTime, Float, ForeignKey, String, Text
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.db import Base


def generate_uuid() -> str:
    """Generate a new UUID string."""
    return str(uuid.uuid4())


class Session(Base):
    """CIPHER conversation session."""

    __tablename__ = "sessions"

    id: Mapped[str] = mapped_column(String(36), primary_key=True, default=generate_uuid)
    session_id: Mapped[str] = mapped_column(String(50), unique=True, index=True)
    turn_number: Mapped[int] = mapped_column(default=0)
    state: Mapped[str] = mapped_column(String(20), default="idle")
    scam_score: Mapped[float] = mapped_column(Float, default=0.0)
    is_scam: Mapped[bool] = mapped_column(Boolean, default=False)
    agent_active: Mapped[bool] = mapped_column(Boolean, default=False)
    persona_id: Mapped[str] = mapped_column(String(50), default="")
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=lambda: datetime.now(timezone.utc))
    updated_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), default=lambda: datetime.now(timezone.utc), onupdate=lambda: datetime.now(timezone.utc)
    )

    messages: Mapped[List["MessageRecord"]] = relationship(
        "MessageRecord", back_populates="session", cascade="all, delete-orphan"
    )
    analysis: Mapped["ScamAnalysis"] = relationship(
        "ScamAnalysis", back_populates="session", uselist=False, cascade="all, delete-orphan"
    )
    entities: Mapped[List["ExtractedEntity"]] = relationship(
        "ExtractedEntity", back_populates="session", cascade="all, delete-orphan"
    )


class MessageRecord(Base):
    """Individual message in a conversation."""

    __tablename__ = "messages"

    id: Mapped[str] = mapped_column(String(36), primary_key=True, default=generate_uuid)
    session_id: Mapped[str] = mapped_column(String(36), ForeignKey("sessions.id"), index=True)
    sender: Mapped[str] = mapped_column(String(50))
    text: Mapped[str] = mapped_column(Text)
    timestamp: Mapped[str] = mapped_column(String(50))
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=lambda: datetime.now(timezone.utc))

    session: Mapped["Session"] = relationship("Session", back_populates="messages")


class ScamAnalysis(Base):
    """Scam analysis result for a message."""

    __tablename__ = "scam_analyses"

    id: Mapped[str] = mapped_column(String(36), primary_key=True, default=generate_uuid)
    session_id: Mapped[str] = mapped_column(String(36), ForeignKey("sessions.id"), index=True)
    message_text: Mapped[str] = mapped_column(Text)
    is_scam: Mapped[bool] = mapped_column(Boolean)
    confidence: Mapped[float] = mapped_column(Float)
    reason: Mapped[str] = mapped_column(Text)
    analyzed_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=lambda: datetime.now(timezone.utc))

    session: Mapped["Session"] = relationship("Session", back_populates="scam_analyses")


class ExtractedEntity(Base):
    """Extracted intelligence entity from conversation.

    Tracks: Phone Numbers, UPI IDs, Crypto Addresses, URLs.
    """

    __tablename__ = "extracted_entities"

    id: Mapped[str] = mapped_column(String(36), primary_key=True, default=generate_uuid)
    session_id: Mapped[str] = mapped_column(String(36), ForeignKey("sessions.id"), index=True)
    entity_type: Mapped[str] = mapped_column(String(50))  # phone, upi, crypto, url
    entity_value: Mapped[str] = mapped_column(Text)
    source_text: Mapped[str] = mapped_column(Text)
    extracted_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=lambda: datetime.now(timezone.utc))

    session: Mapped["Session"] = relationship("Session", back_populates="extracted_entities")


class ScammerProfile(Base):
    """Persistent profile for a scammer (tracked across sessions)."""

    __tablename__ = "scammer_profiles"
    
    id: Mapped[str] = mapped_column(String(36), primary_key=True, default=generate_uuid)
    sender: Mapped[str] = mapped_column(String(50), unique=True, index=True)
    first_seen: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=lambda: datetime.now(timezone.utc))
    last_seen: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=lambda: datetime.now(timezone.utc))
    total_engagements: Mapped[int] = mapped_column(default=0)
    total_turns: Mapped[int] = mapped_column(default=0)
    risk_score: Mapped[float] = mapped_column(Float, default=0.0)
    scam_categories: Mapped[str] = mapped_column(Text, default="[]")  # JSON list
    extracted_entities: Mapped[str] = mapped_column(Text, default="{}")  # JSON dict
    tactics_observed: Mapped[str] = mapped_column(Text, default="[]")  # JSON list
    status: Mapped[str] = mapped_column(String(20), default="active")
