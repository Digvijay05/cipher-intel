"""Logging configuration for Honeypot API.

Provides structured JSON logging for production observability.
"""

import logging
import os
import sys
from logging import Formatter, StreamHandler


class JSONFormatter(Formatter):
    """Simple JSON formatter for structured logging."""

    def format(self, record: logging.LogRecord) -> str:
        """Format log record as JSON string."""
        import json
        from datetime import datetime, timezone

        log_data = {
            "timestamp": datetime.now(timezone.utc).isoformat(),
            "level": record.levelname,
            "logger": record.name,
            "message": record.getMessage(),
        }

        if record.exc_info:
            log_data["exception"] = self.formatException(record.exc_info)

        if hasattr(record, "session_id"):
            log_data["session_id"] = record.session_id

        return json.dumps(log_data)


def setup_logging() -> None:
    """Configure application logging.

    Uses JSON format for production, simple format for development.
    """
    log_level = os.getenv("LOG_LEVEL", "INFO").upper()
    use_json = os.getenv("LOG_FORMAT", "json").lower() == "json"

    # Get root logger
    root_logger = logging.getLogger()
    root_logger.setLevel(getattr(logging, log_level, logging.INFO))

    # Clear existing handlers
    root_logger.handlers.clear()

    # Create console handler
    handler = StreamHandler(sys.stdout)
    handler.setLevel(getattr(logging, log_level, logging.INFO))

    if use_json:
        handler.setFormatter(JSONFormatter())
    else:
        handler.setFormatter(
            Formatter("%(asctime)s - %(name)s - %(levelname)s - %(message)s")
        )

    root_logger.addHandler(handler)

    # Suppress noisy loggers
    logging.getLogger("httpx").setLevel(logging.WARNING)
    logging.getLogger("httpcore").setLevel(logging.WARNING)
    logging.getLogger("openai").setLevel(logging.WARNING)

    logging.info("Logging configured", extra={"level": log_level, "json": use_json})
