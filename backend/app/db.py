"""Database connection management.

Provides async and sync SQLAlchemy engines, session factories,
and database initialization functions.
"""

from contextlib import asynccontextmanager
from typing import AsyncGenerator

from sqlalchemy import create_engine
from sqlalchemy.ext.asyncio import AsyncSession, async_sessionmaker, create_async_engine
from sqlalchemy.orm import DeclarativeBase, sessionmaker

from app.config.settings import settings

# Async engine for runtime
async_engine = create_async_engine(settings.DATABASE_URL, echo=False)
AsyncSessionLocal = async_sessionmaker(async_engine, class_=AsyncSession, expire_on_commit=False)

# Sync engine for migrations/setup
_sync_url = settings.DATABASE_URL.replace("+aiosqlite", "")
sync_engine = create_engine(_sync_url, echo=False)
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
