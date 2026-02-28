"""Centralized application configuration via Pydantic BaseSettings.

All environment variables MUST be declared in this module.
No other file is permitted to read os.getenv or os.environ directly.

Usage:
    from app.config.settings import settings
    settings.CIPHER_API_KEY
"""

from pathlib import Path
from typing import Optional

from pydantic_settings import BaseSettings


class Settings(BaseSettings):
    """Application settings loaded exclusively from .env file.

    Every configurable value — secrets, URLs, thresholds, timeouts —
    must be declared here. No fallback defaults for required secrets.
    """

    # ── Authentication ──────────────────────────────────────────────
    CIPHER_API_KEY: str

    # ── LLM: Ollama (Primary) ──────────────────────────────────────
    OLLAMA_API_KEY: str
    OLLAMA_MODEL: str = "gemma3:27b-cloud"
    OLLAMA_BASE_URL: str = "https://ollama.com"

    # ── LLM: Groq (Fallback) ──────────────────────────────────────
    GROQ_API_KEY: str = ""
    GROQ_MODEL: str = "llama-3.3-70b-versatile"

    # ── LLM: OpenAI (Optional) ────────────────────────────────────
    OPENAI_API_KEY: str = ""

    # ── LLM Inference Tuning ──────────────────────────────────────
    LLM_DEFAULT_TEMPERATURE: float = 0.8
    LLM_DEFAULT_MAX_TOKENS: int = 512
    LLM_REQUEST_TIMEOUT_SECONDS: float = 60.0
    LLM_MAX_RETRIES: int = 3
    LLM_RETRY_DELAY_SECONDS: float = 1.0
    LLM_GENERATION_TIMEOUT_SECONDS: float = 8.0

    # ── Session Management ────────────────────────────────────────
    MAX_SESSION_MESSAGES: int = 20
    REDIS_URL: Optional[str] = None
    REDIS_SESSION_TTL_SECONDS: int = 3600
    REDIS_KEY_PREFIX: str = "cipher:session:"

    # ── Database ──────────────────────────────────────────────────
    DATABASE_URL: str = "sqlite+aiosqlite:///./cipher.db"

    # ── Logging ───────────────────────────────────────────────────
    LOG_LEVEL: str = "INFO"
    LOG_FORMAT: str = "json"

    # ── Agent Orchestration ───────────────────────────────────────
    AGENT_MAX_RETRIES: int = 3
    AGENT_DEFAULT_PERSONA: str = "margaret_72"

    model_config = {
        "env_file": str(Path(__file__).resolve().parents[3] / ".env"),
        "env_file_encoding": "utf-8",
        "case_sensitive": True,
        "extra": "ignore",
    }


settings = Settings()
