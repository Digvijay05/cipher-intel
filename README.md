# Agentic Honeypot API

A backend system designed to engage scammers in automated, believable conversations while extracting intelligence data. The system uses a persona-driven LLM agent to impersonate a vulnerable target, keeping malicious actors engaged while logging UPI IDs, phone numbers, phishing links, and other scam indicators.

## Key Capabilities

- Real-time scam detection via regex-based rule matching with weighted scoring
- Persona-driven conversation agent using GPT-4o-mini via LangChain
- Structured intelligence extraction (UPI IDs, bank accounts, phone numbers, URLs)
- Session state management with Redis or in-memory fallback
- Final callback submission to external evaluation endpoint
- Containerized deployment with non-root user security

---

## System Architecture

```
+------------------+       +----------------------+       +-------------------+
|   HTTP Request   | ----> |   Auth Middleware    | ----> |   Agent Controller|
| POST /api/...    |       |   (x-api-key)        |       |   (Orchestrator)  |
+------------------+       +----------------------+       +-------------------+
                                                                   |
                    +----------------------------------------------+
                    |                      |                       |
                    v                      v                       v
          +----------------+     +------------------+     +------------------+
          | Scam Detector  |     | Intel Extractor  |     | Session Store    |
          | (Regex Rules)  |     | (Regex Patterns) |     | (Redis/InMemory) |
          +----------------+     +------------------+     +------------------+
                    |                      |
                    v                      v
          +----------------+     +------------------+
          | LLM Agent      |     | Callback Client  |
          | (LangChain)    |     | (httpx POST)     |
          +----------------+     +------------------+
```

### Request Flow

1. Incoming POST request authenticated via `x-api-key` header
2. `AgentController` retrieves or creates session state
3. Scam detection runs on message text, returning confidence score
4. If scam detected (`score >= 0.5`): agent activates, LLM generates persona response
5. If not scam: minimal "Okay." response returned
6. Intelligence extracted from every message and merged into session buffer
7. When `message_count >= MAX_SESSION_MESSAGES` and scam detected: final callback sent
8. Session state persisted after each request

---

## Technology Stack

| Component | Technology | Version Constraint |
|-----------|------------|-------------------|
| Framework | FastAPI | >= 0.100.0 |
| LLM Integration | LangChain + langchain-openai | >= 0.1.0 |
| LLM Model | OpenAI GPT-4o-mini | - |
| HTTP Client | httpx | >= 0.25.0 |
| Validation | Pydantic | >= 2.0.0 |
| Session Store | Redis (async) or In-Memory | >= 5.0.0 |
| Database | SQLAlchemy + aiosqlite | >= 2.0.0 |
| Server | Uvicorn | >= 0.23.0 |
| Container | Python 3.10-slim | - |
| Testing | pytest + pytest-asyncio | >= 7.4.0 |

---

## Repository Structure

```
AI_honeypot_Backendmain/
├── app/
│   ├── main.py                 # FastAPI application entry point
│   ├── routes.py               # API endpoint definitions (/health, /api/honeypot/*)
│   ├── config.py               # Environment variable loading
│   ├── db.py                   # SQLAlchemy async/sync database setup
│   ├── db_models.py            # SQLAlchemy ORM models
│   ├── agent/
│   │   ├── controller.py       # Central orchestration logic
│   │   └── prompt.py           # LLM persona prompt and response generation
│   ├── callback/
│   │   └── client.py           # HTTP client for final callback submission
│   ├── intel/
│   │   └── extractor.py        # Regex-based intelligence extraction
│   ├── logging/
│   │   └── config.py           # JSON/simple logging configuration
│   ├── middleware/
│   │   └── auth.py             # API key verification dependency
│   ├── scam_detector/
│   │   ├── rules.py            # Regex patterns with weights
│   │   └── scorer.py           # Score aggregation and threshold logic
│   ├── schemas/
│   │   └── request.py          # Pydantic request/response models
│   └── session/
│       ├── models.py           # SessionState Pydantic model
│       └── store.py            # InMemory and Redis session stores
├── scripts/
│   ├── start-with-ngrok.ps1    # Windows startup script with ngrok
│   └── start-with-ngrok.sh     # Linux/macOS startup script with ngrok
├── tests/
│   ├── e2e_scam_flow.py        # End-to-end lifecycle tests
│   ├── test_api.py             # API endpoint tests
│   ├── test_scam_detector.py   # Scam detection unit tests
│   └── test_session.py         # Session store tests
├── docs/
│   ├── constraints.md          # Persona and output schema constraints
│   └── evaluation_invariants.md
├── .dockerignore               # Docker build exclusions
├── .env.example                # Environment variable template
├── DEPLOYMENT.md               # Complete deployment guide
├── Dockerfile                  # Multi-stage build with health check
├── docker-compose.yml          # Backend + Redis with health checks
├── ngrok.yml.example           # ngrok configuration template
└── requirements.txt            # Python dependencies
```

---

## Core Modules Explained

### AgentController (`app/agent/controller.py`)

Central orchestrator that coordinates all subsystems.

**Responsibilities:**
- Retrieve or create `SessionState` for incoming `session_id`
- Run scam detection and update session flags
- Extract intelligence and merge into cumulative buffer
- Decide response path: minimal reply vs. LLM-generated persona response
- Enforce stop condition (`MAX_SESSION_MESSAGES`) and trigger callback
- Prevent duplicate callbacks via `callback_sent` flag

**Inputs:** `session_id`, `Message`, `conversation_history`
**Outputs:** Response string
**Side Effects:** Session state mutation, callback HTTP POST

### Scam Detector (`app/scam_detector/`)

Regex-based detection engine with weighted scoring.

**Components:**
- `rules.py`: 15+ regex patterns covering UPI IDs, urgency phrases, threats, impersonation, and suspicious URLs
- `scorer.py`: Aggregates matched rule weights, triggers at threshold `0.5`

**Output:** `ScamSignal(is_scam: bool, confidence: float, matched_rules: List[str])`

### Intelligence Extractor (`app/intel/extractor.py`)

Extracts structured data from message text.

**Patterns:**
- UPI IDs: `*@ybl`, `*@paytm`, `*@okaxis`, etc.
- Phone Numbers: Indian format `+91` or `6-9` prefix with 10 digits
- URLs: All HTTP(S) links excluding known safe domains
- Bank Accounts: 9-18 digit numbers when "account" or "bank" mentioned
- Keywords: "otp", "verify", "blocked", "arrest", etc.

**Output:** `ExtractedIntelligence` dataclass with five list fields

### LLM Agent (`app/agent/prompt.py`)

LangChain-based persona agent using OpenAI GPT-4o-mini.

**Persona:** "Margaret" - 72-year-old widow, confused, trusting, not tech-savvy

**Behavioral Rules:**
- Never reveal AI nature
- Never accuse scammer
- Use hesitation markers and ask clarifying questions
- Express anxiety about money without refusing engagement

**Parameters:** `temperature=0.8`, `max_tokens=512`

### Session Store (`app/session/store.py`)

Supports two backends:
- `InMemorySessionStore`: Development/testing
- `RedisSessionStore`: Production with 1-hour TTL

Selection is automatic based on `REDIS_URL` environment variable presence.

### Callback Client (`app/callback/client.py`)

Sends final intelligence to external endpoint.

**Target:** `https://hackathon.guvi.in/api/updateHoneyPotFinalResult`
**Timeout:** 30 seconds
**Payload:**
```json
{
  "sessionId": "abc123-session-id",
  "scamDetected": true,
  "totalMessagesExchanged": 18,
  "extractedIntelligence": {
    "bankAccounts": [],
    "upiIds": ["scammer@upi"],
    "phishingLinks": [],
    "phoneNumbers": ["+91XXXXXXXXXX"],
    "suspiciousKeywords": ["urgent", "verify"]
  },
  "agentNotes": "Scammer used urgency tactics. Attempted UPI payment extraction."
}
```

---

## API Design

### POST `/api/honeypot/message`

Process incoming message and generate honeypot response.

**Headers:**
| Header | Required | Description |
|--------|----------|-------------|
| `x-api-key` | Yes | API authentication key |
| `Content-Type` | Yes | `application/json` |

**Request Body:**
```json
{
  "sessionId": "sess-abc123-def456",
  "message": {
    "sender": "scammer",
    "text": "Your bank account has been suspended!",
    "timestamp": "2026-02-04T21:30:00Z"
  },
  "conversationHistory": [],
  "metadata": {
    "channel": "sms",
    "language": "en",
    "locale": "en-US"
  }
}
```

**Response (200 OK):**
```json
{
  "status": "success",
  "reply": "Oh dear, what should I do? Can you explain more slowly?"
}
```

**Response (401 Unauthorized):**
```json
{
  "detail": "Invalid API Key"
}
```

### GET/POST `/api/honeypot/test`

API test endpoint (requires authentication). Returns status without processing body.

**Response:**
```json
{
  "status": "ok",
  "message": "Honeypot endpoint reachable"
}
```

### GET `/health`

Container health check endpoint. **No authentication required.**

Used by Docker HEALTHCHECK and load balancers.

**Response:**
```json
{
  "status": "healthy"
}
```

---

## Execution Flow

```
1. Request received at POST /api/honeypot/message
   └─> verify_api_key() validates x-api-key header

2. AgentController.process_message() invoked
   └─> Session retrieved from store (or created)

3. detect_scam(message.text) runs
   └─> Pattern matching against 15+ rules
   └─> Score calculated (sum of matched weights)
   └─> is_scam = true if score >= 0.5

4. Session state updated:
   └─> message_count incremented
   └─> scam_score updated to max(current, new)
   └─> agent_active set if scam detected

5. extract_intelligence(message.text) runs
   └─> UPI IDs, phones, URLs, accounts, keywords extracted
   └─> Merged into session.intel_buffer (deduplicated)

6. Decision branch:
   └─> If NOT agent_active: return "Okay."
   └─> If agent_active:
       └─> If message_count >= MAX_SESSION_MESSAGES:
           └─> If scam and not callback_sent:
               └─> send_callback() to external endpoint
               └─> callback_sent = true
           └─> return "I'm sorry, I need to go now..."
       └─> If callback already sent:
           └─> return "I already reported this..."
       └─> Else: agent_reply() generates LLM response

7. Session saved to store

8. Response returned to client
```

---

## Configuration and Environment Variables

| Variable | Required | Default | Description |
|----------|----------|---------|-------------|
| `HONEYPOT_API_KEY` | Yes | - | API key for request authentication |
| `OPENAI_API_KEY` | Yes | - | OpenAI API key for LLM access |
| `MAX_SESSION_MESSAGES` | No | `20` | Message count before forced termination |
| `REDIS_URL` | No | - | Redis connection URL; if unset, uses in-memory store |
| `DATABASE_URL` | No | `sqlite+aiosqlite:///./honeypot.db` | SQLAlchemy database URL |
| `LOG_LEVEL` | No | `INFO` | Logging level (DEBUG, INFO, WARNING, ERROR) |
| `LOG_FORMAT` | No | `json` | Log output format (`json` or `simple`) |

The application exits immediately if required variables (`HONEYPOT_API_KEY`, `OPENAI_API_KEY`) are not set.

---

## Deployment and Running Locally

### Quick Start with Docker + ngrok (Recommended for Evaluation)

```bash
# 1. Copy environment template and fill in secrets
cp .env.example .env
notepad .env   # Windows
# nano .env   # Linux/macOS

# 2. Authenticate ngrok (one-time setup)
ngrok config add-authtoken YOUR_AUTHTOKEN

# 3. Start everything (Windows)
.\scripts\start-with-ngrok.ps1

# 3. Start everything (Linux/macOS)
chmod +x scripts/start-with-ngrok.sh
./scripts/start-with-ngrok.sh
```

This starts Docker containers and creates an ngrok tunnel for public HTTPS access.

### Docker Compose Only

```bash
# Create .env file with required variables
cp .env.example .env
# Edit .env with your actual API keys

# Build and run
docker compose up --build -d

# View logs
docker compose logs -f
```

Services:
- `honeypot-backend`: Application on port 8000 (with health check)
- `redis`: Redis 7 Alpine on port 6379 (with persistence)

### Public Exposure with ngrok

After Docker containers are running:

```bash
# Start ngrok tunnel
ngrok http 8000

# Your public URL will be displayed:
# Forwarding  https://abc123.ngrok-free.app -> http://localhost:8000
```

Use the `https://....ngrok-free.app` URL for GUVI evaluation.

### Local Development

```bash
# Create virtual environment
python -m venv venv

# Activate (Windows)
.\venv\Scripts\activate

# Activate (Linux/macOS)
source venv/bin/activate

# Install dependencies
pip install -r requirements.txt

# Set environment variables
export HONEYPOT_API_KEY=your-api-key
export OPENAI_API_KEY=your-openai-key
export LOG_FORMAT=simple

# Run server
python -m uvicorn app.main:app --reload --port 8000
```

### Running Tests

```bash
# All tests
python -m pytest tests/ -v

# Specific test file
python -m pytest tests/e2e_scam_flow.py -v
```

See [DEPLOYMENT.md](DEPLOYMENT.md) for complete deployment instructions and troubleshooting.

---

## Error Handling and Logging

### Logging Configuration

- **Production (default):** JSON-formatted logs to stdout
- **Development:** Simple format with timestamp, logger name, level, message

Noisy loggers (`httpx`, `httpcore`, `openai`) are suppressed to WARNING level.

JSON log structure:
```json
{
  "timestamp": "2026-02-04T16:00:00+00:00",
  "level": "INFO",
  "logger": "app.agent.controller",
  "message": "Scam detected for session sess-123, agent activated",
  "session_id": "sess-123"
}
```

### Error Handling

| Component | Error Type | Behavior |
|-----------|------------|----------|
| `agent_reply()` | LLM API failure | Returns fallback message, logs error |
| `send_callback()` | Timeout (30s) | Returns `False`, logs error |
| `send_callback()` | Network error | Returns `False`, logs error |
| `RedisSessionStore` | Connection failure | Logs error, returns `None`/`False` |
| `verify_api_key()` | Invalid key | Raises HTTP 401 |
| Config loading | Missing required env | Exits process with error message |

---

## Limitations and Assumptions

### Limitations

- **Single callback per session:** Once `callback_sent` is true, no further callbacks are sent regardless of subsequent messages
- **No callback retry:** Failed callbacks are logged but not retried
- **Regex-only detection:** No ML-based classification; false positives/negatives possible with creative phrasing
- **English-only patterns:** Scam detection rules are designed for English and Indian regional patterns
- **No persistent conversation history:** Conversation context relies on client-provided `conversationHistory`

### Assumptions

- Client provides accurate `sessionId` for session continuity
- Client provides complete `conversationHistory` in each request
- Message timestamps are provided by client in ISO format
- OpenAI API is accessible and responding within reasonable latency
- For production: Redis is available at `REDIS_URL` for session persistence across restarts

### Security Considerations

- API runs as non-root user (`honeypot`) in container
- API key authentication required on all endpoints except test
- No credentials logged; sensitive data in `intel_buffer` is only sent to callback endpoint
- No inbound data persistence to disk in default SQLite; sessions stored in memory or Redis
