# CIPHER

**Conversational Intelligence Platform for Honeypot Engagement & Reporting**

An automated, decentralized agentic AI system that engages scammers in believable text conversations, extracts intelligence (UPI IDs, phone numbers, phishing links), and reports findings.

CIPHER runs an **Autonomous Android App** on the edge (device) to safely intercept and statically score SMS scams offline. If flagged as malicious, it passes the context to a **Scalable FastAPI Cloud Backend** where LLMs dynamically generate persona-driven interactions to waste the scammer's time while aggressively extracting threat intel.

For a full structural breakdown, see [ARCHITECTURE.md](ARCHITECTURE.md).

## Repository Structure

```
cipher-intel/
├── backend/                        # FastAPI REST API (Cloud Intelligence)
│   ├── app/
│   │   ├── main.py                 # FastAPI entrypoint
│   │   ├── api/routes.py           # HTTP endpoints
│   │   ├── core/llm/               # LLM Provider Abstractions (Ollama/Groq/OpenAI)
│   │   ├── models/                 # SQLAlchemy ORM & Pydantic Schemas
│   │   └── services/               # Scam detection, entity extraction, and webhooks
│   ├── tests/                      # pytest test suite
│   ├── Dockerfile                  # Multi-stage production image
│   └── docker-compose.yml          # Container orchestration (API + Redis)
├── frontend/                       # Android App (Edge Honeypot)
│   ├── app/src/main/java/...       # Kotlin Android codebase
│   │   ├── receiver/               # SmsReceiver & SmsDeliveryReceiver
│   │   ├── worker/                 # SmsProcessingWorker & EngagementWorker
│   │   ├── detection/              # LocalValidator (Regex-based offline triage)
│   │   └── api/                    # RetrofitClient (Circuit breakers & HTTPS)
│   └── README.md                   # Detailed frontend compile instructions
├── ARCHITECTURE.md                 # System C4 Diagrams
├── CONTRIBUTING.md                 # Contribution guidelines
├── SECURITY.md                     # Security policy
└── README.md                       # This file
```

## Getting Started

CIPHER operates as a dual-layer system. You will need to spin up the cloud API and compile the Android app pointing to it.

### 1. Cloud Backend Setup

**Prerequisites:** Docker, Docker Compose, and an LLM API key (Ollama Cloud, Groq, or OpenAI).

1. Navigate to the `backend/` directory.
2. Copy `.env.example` to `.env` and fill in your API keys:
   ```bash
   CIPHER_API_KEY=your_secure_authentication_key
   OLLAMA_API_KEY=your_ollama_key  # or GROQ_API_KEY
   ```
3. Boot the environment utilizing Docker:
   ```bash
   docker compose up -d --build
   ```
The backend API is now exposed on `http://localhost:8000` (or your deployment domain).

### 2. Android Frontend Setup

**Prerequisites:** Android Studio, JDK 17+, Android SDK 34.

1. In the project root, ensure your `.env` contains:
   ```bash
   CIPHER_API_KEY=your_secure_authentication_key
   CIPHER_BASE_URL=https://your-backend-domain.com/  # Or ngrok URL if testing locally
   ```
2. Open the `frontend` folder in Android Studio.
3. Sync Gradle. The `build.gradle.kts` will automatically inject your `.env` secrets into the `BuildConfig`.
4. Compile and Run on a physical testing device (Dual-SIM supported).

> [!WARNING]
> Do NOT install the Android Honeypot app on your primary personal device. It intercepts all incoming SMS messages for scoring and may automatically engage with unrecognized numbers scoring high for scams. Use a dedicated burner device.

## Intelligence Reporting

The system sends a final intelligence report to the configured `CIPHER_CALLBACK_URL` when **all three conditions** are met:

1. **`scamDetected = true`** — scam detection engine triggered.
2. **Engagement complete** — session reached `MAX_SESSION_MESSAGES`.
3. **Intelligence extracted** — PII, URLs, or Crypto/UPI accounts extracted during the conversation.

Example webhook payload:
```json
{
  "sessionId": "sess-abc123-def456",
  "scamDetected": true,
  "totalMessagesExchanged": 20,
  "extractedIntelligence": {
    "bankAccounts": [],
    "upiIds": ["scammer@ybl"],
    "phishingLinks": [],
    "phoneNumbers": ["9876543210"],
    "suspiciousKeywords": ["verify", "blocked", "otp"]
  },
  "agentNotes": "Scammer used urgency tactics. Attempted UPI payment extraction."
}
```

## Contributing & Testing

Refer to [CONTRIBUTING.md](CONTRIBUTING.md) for code styling.
Testing the backend:
```bash
cd backend
python -m pytest tests/ -v
```
Testing the Android worker pipeline without a carrier plan:
```powershell
.\frontend\scripts\loopback_test.ps1
```

## License

MIT
