# ==============================================================================
# Dockerfile for CIPHER API
# ==============================================================================
# Multi-stage build with non-root user and health check.
# Production-ready deployment image.
# ==============================================================================

# Stage 1: Build dependencies
FROM python:3.10-slim AS builder

ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1

WORKDIR /build

RUN apt-get update && apt-get install -y --no-install-recommends \
    gcc \
    build-essential \
    libpq-dev \
    && rm -rf /var/lib/apt/lists/*

RUN python -m venv /opt/venv
ENV PATH="/opt/venv/bin:$PATH"

COPY backend/requirements.txt .
RUN pip install --no-cache-dir pip setuptools wheel && \
    pip install --no-cache-dir -r requirements.txt

# Stage 2: Production image
FROM python:3.10-slim

LABEL maintainer="CIPHER Team"
LABEL description="Conversational Intelligence Platform for Honeypot Engagement & Reporting"
LABEL version="1.0.0"

ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1 \
    PATH="/opt/venv/bin:$PATH"

RUN apt-get update && apt-get install -y --no-install-recommends \
    libpq5 \
    && rm -rf /var/lib/apt/lists/*

RUN groupadd -r cipher && useradd -r -g cipher -d /app -s /sbin/nologin cipher

WORKDIR /app
RUN chown cipher:cipher /app

COPY --from=builder /opt/venv /opt/venv

COPY --chown=cipher:cipher backend/app/ ./app/

USER cipher

EXPOSE 8000

HEALTHCHECK --interval=30s --timeout=10s --start-period=15s --retries=3 \
    CMD ["python", "-c", "import urllib.request; urllib.request.urlopen('http://localhost:8000/health')"]

CMD ["python", "-m", "uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000"]
