"""Database connection management.

Per TODO.md Phase 2:
- Choose SQLite (dev) / Postgres (prod).
- Create db.py for connection management.
"""

import os
from contextlib import asynccontextmanager
from typing import AsyncGenerator

from sqlalchemy import create_engine
from sqlalchemy.ext.asyncio import AsyncSession, async_sessionmaker, create_async_engine
from sqlalchemy.orm import DeclarativeBase, sessionmaker

# Database URL - SQLite for dev, can be overridden for Postgres in prod
DATABASE_URL = os.getenv("DATABASE_URL", "sqlite+aiosqlite:///./honeypot.db")
SYNC_DATABASE_URL = DATABASE_URL.replace("+aiosqlite", "").replace("sqlite:", "sqlite:")

# Async engine for runtime
async_engine = create_async_engine(DATABASE_URL, echo=False)
AsyncSessionLocal = async_sessionmaker(async_engine, class_=AsyncSession, expire_on_commit=False)

# Sync engine for migrations/setup
sync_engine = create_engine(
    SYNC_DATABASE_URL.replace("+aiosqlite", ""),
    echo=False,
)
SyncSessionLocal = sessionmaker(bind=sync_engine)


class Base(DeclarativeBase):
    """Base class for SQLAlchemy models."""

    pass


async def get_db() -> AsyncGenerator[AsyncSession, None]:
    """Dependency to get database session.

    Yields:
        AsyncSession: Database session.
    """
    async with AsyncSessionLocal() as session:
        try:
            yield session
            await session.commit()
        except Exception:
            await session.rollback()
            raise


def init_db() -> None:
    """Initialize database tables synchronously."""
    from app.models.db_models import Base  # noqa: F401

    Base.metadata.create_all(bind=sync_engine)


async def init_db_async() -> None:
    """Initialize database tables asynchronously."""
    from app.models.db_models import Base  # noqa: F401

    async with async_engine.begin() as conn:
        await conn.run_sync(Base.metadata.create_all)
