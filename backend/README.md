# CIPHER API Backend

This module houses the FastAPI Python application that powers the Conversational Intelligence Platform for Honeypot Engagement & Reporting (CIPHER). It is responsible for autonomously generating conversational replies to scammers using Cloud LLMs and extracting key intelligence entities (UPI IDs, URLs, Phone Numbers) using strictly structured output techniques.

## Key Abstractions

- **FastAPI / Uvicorn**: The asynchronous HTTP web server handling incoming webhook requests from Android nodes natively over REST `POST /api/honeypot/message`.
- **SQLAlchemy (Async)**: Powers the persistence layer across PostgreSQL/SQLite, saving each generated sentence. Or orchestrates asynchronous connection pooling seamlessly.
- **LLMFactory**: A powerful abstraction bridging native structured JSON inference between differing endpoints (`Ollama Cloud`, `Groq`, and `OpenAI`).
- **Prompt Architecture**: Strict System Instructions and Few-Shot templates bound to the "Margaret" persona to maintain believability across multi-turn sessions (e.g., maintaining memory context of past messages without hallucinating facts).

## Getting Started

### Environment Variables

Ensure your `backend/.env` is hydrated with the minimum configuration keys:

```bash
CIPHER_API_KEY=your_secure_authentication_key  # Required for Node -> API auth
OLLAMA_API_KEY=your_ollama_key                 # Primary LLM provider
```

### Running Locally without Docker

1. Create and Activate Virtual Env:
```bash
python -m venv .venv
.\.venv\Scripts\Activate.ps1
```
2. Install Requirements:
```bash
pip install -r requirements.txt
```
3. Boot the Uvicorn Server:
```bash
python -m uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload
```

## Running Tests

The test suite validates LLM routing, JSON schema extraction accuracy, and edge-handling (Empty bodies, missing headers).

```bash
pytest tests/ -v
```
