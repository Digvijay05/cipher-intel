# CIPHER — Code Design Blueprint

## 1. Repository Structure

```
cipher/
├── services/
│   ├── detection/                    # Scam detection microservice
│   │   ├── app/
│   │   │   ├── main.py               # FastAPI entrypoint
│   │   │   ├── api/routes.py          # /detect, /feedback endpoints
│   │   │   ├── models/schemas.py      # Pydantic request/response
│   │   │   ├── engine/
│   │   │   │   ├── rules.py           # Rule-based classifier
│   │   │   │   ├── ml_model.py        # ML model inference wrapper
│   │   │   │   ├── ensemble.py        # Ensemble scorer (rules + ML)
│   │   │   │   └── features.py        # Feature extraction
│   │   │   ├── core/config.py
│   │   │   └── middleware/auth.py
│   │   ├── tests/
│   │   ├── Dockerfile
│   │   └── pyproject.toml
│   │
│   ├── engagement/                    # Conversation agent microservice
│   │   ├── app/
│   │   │   ├── main.py
│   │   │   ├── api/routes.py          # /converse endpoint
│   │   │   ├── agent/
│   │   │   │   ├── orchestrator.py    # Central coordination
│   │   │   │   ├── persona/           # YAML persona definitions
│   │   │   │   ├── prompts/builder.py # Dynamic prompt assembly
│   │   │   │   ├── memory/            # Conversation summarization
│   │   │   │   └── reflection/        # Response validation
│   │   │   ├── llm/
│   │   │   │   ├── factory.py         # Provider factory
│   │   │   │   ├── ollama.py
│   │   │   │   ├── groq.py
│   │   │   │   └── openai.py
│   │   │   └── session/
│   │   │       ├── store.py           # Redis session management
│   │   │       └── state.py           # Session state machine
│   │   ├── tests/
│   │   ├── Dockerfile
│   │   └── pyproject.toml
│   │
│   ├── intelligence/                  # Intelligence extraction & graph
│   │   ├── app/
│   │   │   ├── main.py
│   │   │   ├── api/routes.py          # /extract, /query, /export
│   │   │   ├── extraction/
│   │   │   │   ├── regex.py           # Pattern-based extraction
│   │   │   │   ├── nlp.py            # NLP-based extraction (future)
│   │   │   │   └── merger.py         # Cross-session deduplication
│   │   │   ├── graph/
│   │   │   │   ├── neo4j_client.py
│   │   │   │   ├── queries.py
│   │   │   │   └── schema.py
│   │   │   └── export/
│   │   │       ├── stix.py           # STIX format export
│   │   │       └── csv.py
│   │   ├── tests/
│   │   ├── Dockerfile
│   │   └── pyproject.toml
│   │
│   ├── analytics/                     # Metrics, dashboards, reporting
│   ├── notification/                  # Push notifications, email, webhooks
│   └── gateway/                       # API gateway configuration
│
├── mobile/
│   ├── android/                       # Android app (Kotlin)
│   │   ├── app/src/main/java/com/cipher/security/
│   │   │   ├── data/                  # Room entities, DAOs, database
│   │   │   ├── domain/               # Repository, use cases
│   │   │   ├── worker/               # WorkManager workers
│   │   │   ├── receiver/             # BroadcastReceivers
│   │   │   ├── api/                  # Retrofit client, models
│   │   │   ├── ui/                   # Compose UI, ViewModels
│   │   │   ├── notification/         # NotificationHelper
│   │   │   └── service/              # Foreground services
│   │   └── build.gradle.kts
│   └── ios/                           # iOS app (Swift, future)
│
├── shared/
│   ├── proto/                         # Protobuf definitions (if using gRPC)
│   ├── schemas/                       # JSON Schema definitions
│   └── events/                        # Event type definitions
│
├── infra/
│   ├── terraform/                     # Cloud infrastructure
│   │   ├── modules/
│   │   │   ├── database/
│   │   │   ├── cache/
│   │   │   ├── kubernetes/
│   │   │   └── networking/
│   │   ├── environments/
│   │   │   ├── dev/
│   │   │   ├── staging/
│   │   │   └── production/
│   │   └── main.tf
│   ├── helm/                          # Kubernetes Helm charts
│   │   ├── cipher-detection/
│   │   ├── cipher-engagement/
│   │   └── cipher-intelligence/
│   └── docker-compose.yml             # Local development
│
├── pipelines/
│   ├── training/                      # ML training pipelines
│   │   ├── data_prep.py
│   │   ├── train_detection.py
│   │   └── evaluate.py
│   └── etl/                           # Data movement pipelines
│
├── .github/
│   └── workflows/
│       ├── ci.yml                     # Lint, test, build
│       ├── cd-staging.yml             # Deploy to staging
│       └── cd-production.yml          # Deploy to production
│
└── docs/
    ├── product_vision.md
    ├── PRD.md
    ├── TDD.md
    ├── CDB.md
    └── api/                           # OpenAPI specs
```

---

## 2. Service Boundaries

| Service | Owns | Consumes | Publishes |
|---------|------|----------|-----------|
| **Detection** | Classification models, scoring rules, feature store | `sms.received` events | `scam.detected`, `message.safe` |
| **Engagement** | Personas, prompts, session state, LLM orchestration | `scam.detected` events | `engagement.turn`, `engagement.completed` |
| **Intelligence** | Entity extraction, knowledge graph, export formats | `engagement.turn` events | `intel.extracted`, `campaign.identified` |
| **Analytics** | Metrics aggregation, dashboards, reporting | All events (read-only) | `report.generated` |
| **Notification** | Push tokens, delivery tracking | `scam.detected`, `engagement.completed` | `notification.sent` |
| **Gateway** | Rate limits, auth, routing | HTTP requests | Access logs |

### Dependency Rule
Services communicate **only** via: (a) events through the event bus, or (b) well-defined HTTP/gRPC APIs. No shared databases. No direct imports across service boundaries.

---

## 3. Domain Model Separation

```
┌─────────────────────────────────────────────────────┐
│                    Domain Layer                      │
│  Pure business logic. No framework dependencies.    │
│  - ScamClassifier, EngagementStrategy, IntelEntity  │
└─────────────────┬───────────────────────────────────┘
                  │ depends on
┌─────────────────▼───────────────────────────────────┐
│                 Application Layer                    │
│  Use cases, orchestration, transaction boundaries.  │
│  - ProcessIncomingSms, ConductEngagement, ExtractIntel│
└─────────────────┬───────────────────────────────────┘
                  │ depends on
┌─────────────────▼───────────────────────────────────┐
│               Infrastructure Layer                   │
│  Framework-specific. Adapters for external systems. │
│  - FastAPI routes, Redis client, Neo4j driver,      │
│    Retrofit client, Room DAOs, WorkManager          │
└─────────────────────────────────────────────────────┘
```

### Key Domain Objects

```python
# Detection domain
@dataclass(frozen=True)
class ClassificationResult:
    scam_detected: bool
    confidence: float        # 0.0 - 1.0
    risk_level: str          # low, medium, high, critical
    matched_rules: list[str]
    features: dict[str, float]

# Engagement domain
@dataclass(frozen=True)
class ConversationTurn:
    session_id: str
    turn_number: int
    speaker: str             # "scammer" | "agent"
    text: str
    timestamp: datetime
    internal_reasoning: str | None  # Agent's private reasoning

# Intelligence domain
@dataclass(frozen=True)
class IntelEntity:
    entity_type: str         # "upi" | "phone" | "domain" | "bank_account"
    value: str
    confidence: float
    source_session: str
    first_seen: datetime
    context: str             # Surrounding text that contained this entity
```

---

## 4. Interface Contracts

### Detection Service API
```yaml
POST /api/v1/detect:
  request:
    message_text: string (required)
    sender: string (required)
    metadata: { channel: string, device_id: string }
  response:
    scam_detected: boolean
    confidence: float
    risk_level: enum(low, medium, high, critical)
    classification_id: uuid

POST /api/v1/detect/feedback:
  request:
    classification_id: uuid
    correct_label: boolean
    correction_reason: string (optional)
  response:
    acknowledged: boolean
```

### Engagement Service API
```yaml
POST /api/v1/engage:
  request:
    session_id: string (required)
    message: { sender: string, text: string, timestamp: int }
    conversation_history: array[message]
    metadata: { channel: string }
  response:
    status: enum(continue, completed, error)
    reply: string (nullable)
    session_state: enum(engaging, completing, completed)
    turn_number: int

GET /api/v1/engage/{session_id}:
  response:
    session_id: string
    state: string
    turn_count: int
    started_at: datetime
    last_activity: datetime
    intelligence_summary: { entities_found: int, entity_types: list }
```

### Intelligence Service API
```yaml
GET /api/v1/intel/query:
  params:
    entity_type: enum(upi, phone, domain, bank_account)
    value: string (optional, exact match)
    since: datetime (optional)
    limit: int (default 50, max 500)
  response:
    results: array[IntelEntity]
    total_count: int
    next_cursor: string

GET /api/v1/intel/campaign/{campaign_id}:
  response:
    campaign: { name, pattern, first_seen, last_seen }
    entities: array[IntelEntity]
    related_campaigns: array[{ id, name, shared_entities: int }]

POST /api/v1/intel/export:
  request:
    format: enum(json, csv, stix)
    entity_types: array[string]
    date_range: { from, to }
  response:
    download_url: string (signed, 1hr expiry)
```

---

## 5. Async Patterns

### Backend (Python/FastAPI)
```python
# All I/O operations are async
async def conduct_engagement(session_id: str, message: Message) -> EngagementResult:
    # Parallel: session lookup + intelligence extraction
    session_task = asyncio.create_task(session_store.get(session_id))
    intel_task = asyncio.create_task(extract_intelligence(message.text))

    session, intel = await asyncio.gather(session_task, intel_task)

    # Sequential: LLM call (depends on session context)
    reply = await orchestrator.generate_response(
        persona_id=session.persona_id,
        session_context=session.to_context(),
        detection_state=session.detection_state
    )

    # Fire-and-forget: event publication
    asyncio.create_task(publish_event("engagement.turn", {...}))

    return EngagementResult(reply=reply, session_state=session.state)
```

### Android (Kotlin Coroutines)
```kotlin
// WorkManager for reliability, Coroutines for concurrency
class EngagementWorker : CoroutineWorker {
    override suspend fun doWork() = withContext(Dispatchers.IO) {
        // All Room + Retrofit calls are suspend functions
        // WorkManager handles lifecycle, retry, constraints
    }
}
```

---

## 6. Idempotency Handling

| Operation | Idempotency Key | Strategy |
|-----------|-----------------|----------|
| SMS processing | SHA-256(sender + body + timestamp) | Dedup in Room before processing |
| Engagement turn | `{session_id}:{turn_number}` | Check turn number in session state |
| Intelligence extraction | `{entity_type}:{value}` | Upsert with `ON CONFLICT` / set merge |
| Event publishing | `event_id` (UUID) | Event bus dedup window (5 min) |
| API key creation | `{tenant_id}:{key_name}` | Database unique constraint |

---

## 7. Background Task Orchestration

### Android (WorkManager)
```
SmsReceiver (BroadcastReceiver, milliseconds)
    → enqueue SmsProcessingWorker (OneTimeWork, IO dispatcher)
        → enqueue EngagementWorker (OneTimeWork, exponential backoff)
        → schedule CleanupWorker (PeriodicWork, 24h interval)
```

### Backend (Celery / ARQ / native asyncio)
```
Event consumed from bus
    → async task handler (bounded concurrency via semaphore)
    → retry with backoff on transient failure
    → dead letter queue after max retries
```

---

## 8. Testing Strategy

### Unit Tests (per service, <1 min)
- Domain logic: pure functions with deterministic inputs
- Mocked external dependencies (Redis, DB, LLM)
- Property-based testing for extraction regexes (Hypothesis)
- Target: >90% coverage on domain/application layers

### Integration Tests (per service, <5 min)
- Testcontainers for Redis, PostgreSQL, Neo4j
- Real HTTP calls to service endpoints
- Event bus integration (produce → consume → verify)
- LLM calls mocked with recorded responses (VCR pattern)

### End-to-End Tests (<15 min)
- Docker Compose full stack
- Simulated SMS → Detection → Engagement → Intelligence → Graph
- Verify entity appears in Neo4j after engagement completion

### Load Tests (scheduled, weekly)
- k6 / Locust against staging environment
- Scenarios: 1000 concurrent detections, 100 concurrent engagements
- Verify p99 latency budgets hold under load

### Chaos Tests (quarterly)
- Kill random service pods during engagement
- Verify session recovery and engagement continuity
- Network partition between services
- Redis eviction under memory pressure

---

## 9. CI/CD Model

```
Pull Request
    → Lint (ruff, ktlint)
    → Unit Tests (parallel per service)
    → Build Docker images
    → Integration Tests (testcontainers)
    → Security Scan (Semgrep, Trivy)
    → PR Review (required 1 approval)

Merge to main
    → Build + tag images (SHA-based)
    → Deploy to staging (ArgoCD sync)
    → E2E tests against staging
    → Manual gate for production

Production deploy
    → Canary (5% traffic for 15 min)
    → Progressive rollout (25% → 50% → 100%)
    → Automated rollback on error rate spike
```

---

## 10. Infrastructure as Code Strategy

### Terraform Module Structure
```
infra/terraform/
├── modules/
│   ├── networking/        # VPC, subnets, security groups
│   ├── kubernetes/        # EKS/GKE cluster, node pools
│   ├── database/          # PostgreSQL (RDS/CloudSQL)
│   ├── cache/             # Redis (ElastiCache/Memorystore)
│   ├── graph/             # Neo4j (self-managed on K8s or Aura)
│   ├── storage/           # S3/GCS buckets
│   ├── monitoring/        # CloudWatch/Stackdriver + Grafana
│   └── secrets/           # Vault / cloud-native secrets
├── environments/
│   ├── dev/main.tf        # Minimal resources, shared infra
│   ├── staging/main.tf    # Production-like, smaller scale
│   └── production/main.tf # Full scale, multi-AZ, HA
└── main.tf                # Provider configuration
```

### Principles
- **No clickops**: All infrastructure changes via PR to `infra/`
- **State locking**: Terraform state in remote backend (S3 + DynamoDB lock)
- **Drift detection**: Scheduled `terraform plan` in CI to detect manual changes
- **Blast radius**: Separate state files per environment and per module
