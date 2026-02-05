# Project Analysis & Roadmap

## 1. High-Level Project Summary

**Project**: AI Honeypot Backend
**Purpose**: A backend system designed to engage scammers in automated, believable conversations to waste their time ("honeypot") and extract intelligence from their behavior.
**Target Users**: Fraud detection teams, security researchers, or automated protection systems protecting end-users.
**Core Capabilities**:
- **Scam Detection**: Analyze incoming messages to detect fraud intent.
- **Agentic Engagement**: autonomously reply to confirmed scammers using an AI persona.
- **Intelligence Extraction**: (Implied/Planned) Extract actionable data (metrics, patterns) from scammer interactions.
**Architecture**: FastAPI-based REST API with a clear separation of concerns (Routes → Auth → Logic/Agent). Designed for stateless or semi-stateless operation (current implementation is stateless).

## 2. Current State Analysis

### Documentation vs. Implementation
| Component | Documentation (Implied/README) | Actual Implementation | Status |
|-----------|--------------------------------|-----------------------|--------|
| **API Surface** | `POST /api/honeypot/message`, `GET /api/honeypot/test` | Fully implemented in `routes.py`. | ✅ Aligned |
| **Auth** | `x-api-key` header | Implemented in `auth.py`. | ✅ Aligned |
| **AI Agent** | "Agentic" conversational replies | **Hardcoded placeholder** ("Why is my account being suspended?"). | ❌ Missing |
| **Scam Detection** | implied AI/Logic | Basic keyword counting (list of 7 words). | ⚠️ Primitive |
| **Intelligence** | `extractor.py` (implied) | Empty function returning `{}`. | ❌ Missing |
| **Persistence** | Implied (Conversation History) | In-memory only (request payload), no DB. | ⚠️ Gap |

### API Completeness Matrix
- **`POST /message`**: Exists, but logic is mocked.
- **`GET /test`**: Functional.
- **Auth**: Functional (simple token match).

## 3. Key Risks & Gaps

### Critical (Blocking)
1.  **Fake "AI" Logic**: The `agent.py` does not call any LLM. It returns a static string. The system currently provides zero value as a honeypot.
2.  **Missing Dependencies**: `requirements.txt` lacks the AI libraries (e.g., `openai`, `langchain`) hints at in `config.py`.
3.  **No Persistence**: Chat history is passed in the request but not stored. Intelligence is lost immediately involved.

### High Priority
1.  **Fragile Scam Detection**: The keyword counter (`score >= 2`) is easily bypassed and produces high false positives/negatives.
2.  **Documentation access**: The PDF specs in `/docs` were not machine-readable during analysis, posing a risk of missed subtle requirements.

### Medium Priority
1.  **Security**: API Key is loaded from env but defaults to `test-secret-key`.
2.  **Observability**: No logging configured (relies on stdout).

## 4. Project Roadmap

### Phase 0: Foundation Fixes (Immediate)
**Objective**: Fix broken dependencies and configuration to prep for real logic.
- **Tasks**: Update `requirements.txt`, proper Env verification.

### Phase 1: Core AI Implementation
**Objective**: Replace mocks with real LLM calls.
- **Scope**: `agent.py` (OpenAI integration), `scam_detector.py` (LLM-based classification).
- **APIs**: `POST /message` becomes functional.

### Phase 2: Intelligence & Persistence
**Objective**: Store data and extract insights.
- **Scope**: Add Database (SQLite/Postgres), Implement `extractor.py` to parse intent/entities.

### Phase 3: Production Hardening
**Objective**: Security, Logging, and robust error handling.
- **Scope**: Docker optimization, structured logging, rate limiting.
