# Ordered Master TODO List

## Phase 0: Foundation & Dependencies (P0)

- [x] **[Infra] Update `requirements.txt`**
  - Add `openai`, `langchain`, `httpx`, `python-dotenv`.
  - Pin versions (e.g., `openai>=1.0.0`).
  - Complexity: S
- [x] **[Config] Secure Environment Variables**
  - Update `config.py` to enforce `OPENAI_API_KEY` presence (fail if missing).
  - Remove default values for secrets.
  - Complexity: S

## Phase 1: Core AI Implementation (P0)

- [x] **[API] Implement Real Scam Detection (`scam_detector.py`)**
  - Replace keyword counting with an LLM call (e.g., "Analyze this text for scam intent: {text}").
  - Return boolean + confidence score + reason.
  - Complexity: M
- [x] **[API] Implement Agent Personality (`agent.py`)**
  - Integrate OpenAI ChatCompletion.
  - Create a system prompt for the "Honeypot Persona" (naive victim, elderly person, etc.).
  - Implement `agent_reply(message_history)` function.
  - Complexity: M
- [x] **[API] Connect Routes to AI Logic**
  - Update `routes.py` to await the new async `agent_reply`.
  - Pass conversation history from request to the agent.
  - Complexity: S

## Phase 2: Persistence & Intelligence (P1)

- [x] **[Infra] Add Database**
  - Choose SQLite (dev) / Postgres (prod).
  - Add `sqlalchemy` or `tortoise-orm`.
  - Create `db.py` for connection management.
  - Complexity: M
- [x] **[Data] Create Data Models**
  - Create `Session`, `Message`, `ScamAnalysis` tables.
  - Update `models.py` to reflect DB schema.
  - Complexity: M
- [x] **[API] Implement Intelligence Extraction (`extractor.py`)**
  - Create functionality to parse conversation for: Phone Numbers, UPI IDs, Crypto Addresses, URLs.
  - Trigger extraction after each message or session end.
  - Save extracted entities to DB.
  - Complexity: L

## Phase 3: Testing & Hardening (P2)

- [x] **[Testing] Add Pytest Suite**
  - Create `tests/` folder.
  - Write unit tests for `scam_detector.py` (mocking OpenAI).
  - Write integration tests for `/api/honeypot/message`.
  - Complexity: M
- [x] **[Infra] Docker Optimization**
  - Update `Dockerfile` to multi-stage build if needed.
  - Add strict user permissions (non-root).
  - Complexity: S
- [x] **[Docs] Update OpenAPI Docs**
  - Add examples to `HoneypotRequest` and `HoneypotResponse` schema in `main.py`.
  - Complexity: S
