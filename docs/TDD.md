# CIPHER — Technical Design Document

## 1. System Architecture Overview

```
┌──────────────────────────────────────────────────────────────────┐
│                         CLIENT TIER                              │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌────────────────┐   │
│  │ Android  │  │   iOS    │  │ Browser  │  │  Enterprise    │   │
│  │   App    │  │   App    │  │Extension │  │  API Client    │   │
│  └─────┬────┘  └─────┬────┘  └────┬─────┘  └───────┬────────┘   │
└────────┼─────────────┼───────────┼──────────────────┼────────────┘
         │             │           │                  │
         └─────────────┴───────┬───┴──────────────────┘
                               │ HTTPS/WSS
┌──────────────────────────────┼───────────────────────────────────┐
│                         API GATEWAY                              │
│  ┌────────────────────────────────────────────────────────────┐   │
│  │  Kong / AWS API Gateway / Traefik                         │   │
│  │  Rate Limiting · JWT Validation · Tenant Routing          │   │
│  └────────────────────────────────────────────────────────────┘   │
└──────────────────────────────┼───────────────────────────────────┘
                               │
┌──────────────────────────────┼───────────────────────────────────┐
│                      SERVICE TIER                                │
│                                                                  │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────────────┐    │
│  │  Detection   │  │  Engagement  │  │   Intelligence       │    │
│  │  Service     │  │  Service     │  │   Service            │    │
│  │              │  │              │  │                       │    │
│  │ - classify() │  │ - converse() │  │ - extract()          │    │
│  │ - score()    │  │ - persona()  │  │ - graph_query()      │    │
│  │ - feedback() │  │ - reflect()  │  │ - export()           │    │
│  └──────┬───────┘  └──────┬───────┘  └───────────┬───────────┘   │
│         │                 │                      │               │
│  ┌──────┴─────────────────┴──────────────────────┴───────────┐   │
│  │                    EVENT BUS                               │   │
│  │               (Kafka / Redis Streams)                      │   │
│  │                                                            │   │
│  │  Topics:                                                   │   │
│  │    sms.received · scam.detected · engagement.turn          │   │
│  │    intel.extracted · session.completed · model.feedback     │   │
│  └────────────────────────────────────────────────────────────┘   │
│                                                                  │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────────────┐    │
│  │  Session     │  │  Notification│  │   Analytics          │    │
│  │  Service     │  │  Service     │  │   Service            │    │
│  └──────────────┘  └──────────────┘  └──────────────────────┘    │
└──────────────────────────────────────────────────────────────────┘
                               │
┌──────────────────────────────┼───────────────────────────────────┐
│                       DATA TIER                                  │
│                                                                  │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌────────────────┐   │
│  │ Postgres │  │  Redis   │  │  Neo4j   │  │  Object Store  │   │
│  │ (OLTP)   │  │ (Cache/  │  │ (Intel   │  │  (S3/GCS)      │   │
│  │          │  │  Session)│  │  Graph)  │  │  Transcripts   │   │
│  └──────────┘  └──────────┘  └──────────┘  └────────────────┘   │
│                                                                  │
│  ┌──────────────────────────────────────────────────────────────┐ │
│  │  ClickHouse / TimescaleDB (Analytics OLAP)                  │ │
│  └──────────────────────────────────────────────────────────────┘ │
└──────────────────────────────────────────────────────────────────┘
```

---

## 2. Event-Driven System Design

All inter-service communication is event-driven. Services are decoupled via an event bus (Kafka in production, Redis Streams for MVP).

### Event Schema

```json
{
  "event_id": "uuid-v4",
  "event_type": "scam.detected",
  "timestamp": "2026-02-28T15:00:00Z",
  "tenant_id": "tenant-abc",
  "payload": { ... },
  "metadata": {
    "source_service": "detection-service",
    "correlation_id": "uuid-v4",
    "version": "1.0"
  }
}
```

### Event Flow

```
sms.received
  → Detection Service consumes, emits:
      scam.detected (if true) OR message.safe

scam.detected
  → Engagement Service consumes, emits:
      engagement.started
      engagement.turn (per exchange)
      engagement.completed

engagement.turn
  → Intelligence Service consumes, emits:
      intel.extracted (per entity found)

engagement.completed
  → Analytics Service consumes (metrics)
  → Intelligence Service (graph finalization)
  → Notification Service (user report)
```

---

## 3. Detection Engine Evolution

### Phase 1: Rule-Based (Current)
- Keyword matching, regex patterns, URL heuristics
- Configurable weighted scoring
- ~85% precision, ~70% recall

### Phase 2: Supervised ML (Month 3-6)
- Train on proprietary labeled dataset (engagement-confirmed scams)
- Features: TF-IDF, sender reputation, temporal patterns, URL structure
- Model: Gradient Boosted Trees (XGBoost) for interpretability
- Target: >95% precision, >85% recall

### Phase 3: Self-Learning (Month 12+)
- Active learning from engagement outcomes (scammer continued → confirmed scam)
- Embedding-based similarity (Sentence-BERT) for new scam template detection
- Reinforcement learning on engagement strategy optimization
- Federated learning for on-device model improvement without raw data collection

### Model Serving Architecture
```
Detection Request
    → Feature Extraction (CPU, <10ms)
    → Model Inference (CPU/GPU, <50ms)
    → Calibration + Threshold (CPU, <5ms)
    → Decision + Confidence Score
    → Total: <100ms p99
```

---

## 4. Agent Orchestration Layer

```
                    ┌─────────────────────┐
                    │  AgentOrchestrator   │
                    │                     │
                    │  1. Load Persona    │
                    │  2. Compress Memory │
                    │  3. Build Prompt    │
                    │  4. LLM Call        │
                    │  5. Reflect/Validate│
                    │  6. Retry if needed │
                    └─────────┬───────────┘
                              │
            ┌─────────────────┼──────────────────┐
            │                 │                  │
    ┌───────▼───────┐ ┌──────▼──────┐ ┌────────▼────────┐
    │ PersonaEngine │ │ PromptBuilder│ │ ReflectionEngine│
    │               │ │             │ │                 │
    │ YAML profiles │ │ Dynamic     │ │ JSON schema     │
    │ Trait mixing  │ │ ChatML      │ │ validation      │
    │ Age/voice/    │ │ Context     │ │ Guardrail check │
    │ tech level    │ │ injection   │ │ Retry handler   │
    └───────────────┘ └─────────────┘ └─────────────────┘
                              │
                    ┌─────────▼───────────┐
                    │  LLM Provider       │
                    │  Factory            │
                    │                     │
                    │  Primary: Ollama    │
                    │  Fallback: Groq     │
                    │  Emergency: OpenAI  │
                    └─────────────────────┘
```

### LLM Provider Failover
1. Primary call with 8s timeout
2. On timeout/error → fallback provider
3. On double failure → cached micro-response ("Sorry, my phone is acting up")
4. All calls logged for latency tracking and cost attribution

---

## 5. Conversation Memory Architecture

### Per-Session (Hot Path)
- **Store**: Redis with JSON serialization
- **TTL**: 1 hour, touch-on-access
- **Content**: Full message history, session state, extracted intel buffer, persona config
- **Compression**: After 10 messages, older messages are summarized by the MemorySummarizer

### Per-Scammer (Warm Path)
- **Store**: PostgreSQL
- **Content**: Scammer phone number → list of session IDs, historical engagement patterns
- **Use**: If the same scammer contacts a different user, system knows their tactics

### Cross-Session Intelligence (Cold Path)
- **Store**: Neo4j graph database
- **Content**: Entity relationships extracted across all engagements
- **Query**: Cypher queries for campaign clustering, network analysis

---

## 6. Intelligence Graph Storage Model

### Technology: Neo4j (production) / SQLite FTS (MVP)

### Schema
```cypher
// Node types
(:PhoneNumber {number, firstSeen, lastSeen, engagementCount})
(:UPIEndpoint {id, provider, firstSeen, lastSeen, riskScore})
(:PhishingDomain {domain, registrar, firstSeen, isActive})
(:BankAccount {number, ifsc, bank, firstSeen})
(:ScamCampaign {name, pattern, firstSeen, lastSeen, victimCount})
(:Engagement {sessionId, startTime, endTime, turnsCount, intelYield})

// Edge types
(:PhoneNumber)-[:SENT_SCAM]->(:Engagement)
(:Engagement)-[:EXTRACTED]->(:UPIEndpoint)
(:Engagement)-[:EXTRACTED]->(:PhishingDomain)
(:UPIEndpoint)-[:BELONGS_TO]->(:ScamCampaign)
(:PhoneNumber)-[:OPERATES_IN]->(:ScamCampaign)
(:PhoneNumber)-[:CO_OCCURS_WITH]->(:PhoneNumber)
```

### Query Examples
```cypher
// Find all UPI endpoints linked to a known scam number
MATCH (p:PhoneNumber {number: "9876543210"})-[:SENT_SCAM]->(e:Engagement)-[:EXTRACTED]->(u:UPIEndpoint)
RETURN DISTINCT u.id, u.provider, u.riskScore

// Cluster scam campaigns by shared infrastructure
MATCH (c1:ScamCampaign)<-[:BELONGS_TO]-(u:UPIEndpoint)-[:BELONGS_TO]->(c2:ScamCampaign)
WHERE c1 <> c2
RETURN c1.name, c2.name, COUNT(u) AS sharedEndpoints
ORDER BY sharedEndpoints DESC
```

---

## 7. Multi-Tenant Isolation Strategy

| Layer | Isolation | Implementation |
|-------|-----------|---------------|
| API | Tenant ID in JWT claims | API gateway validates and injects tenant context |
| Database | Schema-per-tenant (PostgreSQL) | Connection pool per schema, no cross-tenant queries |
| Redis | Key prefix (`tenant:{id}:session:{sid}`) | Namespace isolation |
| Neo4j | Property-based (`tenant_id` on all nodes) | Query filter enforcement at service layer |
| Object Storage | Bucket-per-tenant or prefix isolation | IAM policy enforcement |
| LLM Calls | No isolation needed (stateless) | Cost attribution via tenant metadata |

---

## 8. Rate Limiting & Abuse Prevention

| Surface | Limit | Mechanism |
|---------|-------|-----------|
| API Gateway | 100 req/min per API key | Token bucket (Kong/redis) |
| SMS Engagement | 1 outbound SMS per 10s per session | Application-level check |
| Concurrent Engagements | 5 per user | Room DB query before creation |
| LLM Calls | 30 calls/min per tenant | Backend rate limiter |
| Intelligence API | Tier-based (10K-unlimited/month) | API key quota tracking |
| Graph Queries | 100/hour for free tier | Application-level counter |

### Abuse Vectors & Mitigations
- **Scammer detection evasion**: Ensemble model with multiple feature classes
- **Intentional false reports**: Require minimum detection confidence before engagement
- **API scraping**: Response pagination limits, IP-based throttling, query complexity limits
- **Cost attack via LLM**: Per-tenant LLM budget cap, fallback to cached responses

---

## 9. Observability Stack

### Logging
- **Format**: Structured JSON (timestamp, level, service, correlation_id, tenant_id, message)
- **Pipeline**: App → Fluent Bit → Elasticsearch → Kibana
- **Retention**: 30 days hot, 90 days warm, 1 year cold (S3)

### Metrics
- **Collection**: Prometheus (pull-based)
- **Visualization**: Grafana
- **Key Metrics**:
  - `detection_latency_ms` (p50, p95, p99)
  - `engagement_turn_latency_ms`
  - `llm_call_duration_ms` by provider
  - `intelligence_extraction_rate` (entities per engagement)
  - `circuit_breaker_state` per upstream
  - `active_sessions_gauge`

### Tracing
- **Protocol**: OpenTelemetry
- **Backend**: Jaeger / Tempo
- **Span Coverage**: API request → detection → engagement → LLM call → extraction → response

### Alerting
- LLM error rate > 5% for 5 minutes → PagerDuty
- Detection latency p99 > 500ms → Slack
- Circuit breaker OPEN for > 2 minutes → PagerDuty
- Engagement completion rate drops > 20% week-over-week → Slack

---

## 10. Resilience Patterns

| Pattern | Implementation |
|---------|---------------|
| Circuit Breaker | 3-state (CLOSED/OPEN/HALF_OPEN), per upstream service, 30s open duration |
| Retry with Backoff | Exponential backoff (500ms base, 3 max retries), jitter to prevent thundering herd |
| Timeout | 8s LLM calls, 5s DB queries, 10s external HTTP |
| Bulkhead | Separate thread pools for detection vs engagement vs intelligence |
| Fallback | Cached micro-responses when LLM unavailable, local detection when API down |
| Idempotency | Message hash deduplication, idempotency keys on engagement turns |
| Graceful Degradation | Detection always works → Engagement degrades first → Notifications last |

---

## 11. Scalability Model

### Horizontal Scaling
- All API services are stateless → scale by adding pods
- Session state in Redis → shared across instances
- Database read replicas for query-heavy intelligence API
- LLM calls are embarrassingly parallel

### Capacity Planning
| Component | 10K Users | 100K Users | 1M Users |
|-----------|-----------|------------|----------|
| API Pods | 2 | 6 | 20 |
| Redis (memory) | 512MB | 4GB | 32GB |
| PostgreSQL | 1 primary + 1 replica | 1 primary + 2 replicas | Sharded (by tenant) |
| Neo4j | Single instance | HA cluster (3 nodes) | Causal cluster (5 nodes) |
| LLM inference | Shared cloud API | Dedicated endpoint | Self-hosted + cloud burst |

---

## 12. Cloud-Agnostic Deployment

### Container Strategy
- All services containerized with multi-stage Docker builds
- Orchestration: Kubernetes (EKS/GKE/AKS or self-managed)
- Helm charts for all services with environment-specific values

### Infrastructure as Code
- Terraform modules for cloud resources (database, cache, object store, networking)
- Provider-agnostic abstractions where possible
- GitOps with ArgoCD for deployment automation

### Environment Parity
| Environment | Purpose | Scale |
|-------------|---------|-------|
| `dev` | Local development | Docker Compose, SQLite, in-memory Redis |
| `staging` | Integration testing | K8s, managed Postgres, managed Redis |
| `production` | User-facing | K8s (multi-AZ), managed everything, CDN |

---

## 13. Data Pipeline for Model Training

```
Raw Events (Kafka)
    → Stream Processing (Flink / Spark Structured Streaming)
    → Feature Store (Feast / custom on PostgreSQL)
    → Training Pipeline (Kubeflow / Airflow)
    → Model Registry (MLflow)
    → Canary Deployment (shadow mode → 5% traffic → 50% → 100%)
    → Feedback Loop (engagement outcomes → labeled data)
```

### Training Data Sources
- Engagement transcripts (with outcome labels: scam confirmed, false positive, inconclusive)
- Detection labeledscoring history (message features + human/automated labels)
- Intelligence graph snapshots (campaign lifecycle patterns)

---

## 14. Security Architecture

### Authentication Flow
```
Client → API Gateway (TLS termination)
    → JWT validation (RS256, short-lived access token)
    → Tenant context extraction
    → Service-to-service: mTLS + service mesh (Istio)
```

### Key Management
- Secrets in HashiCorp Vault (or cloud-native: AWS Secrets Manager / GCP Secret Manager)
- Automatic rotation: API keys (90 days), JWT signing keys (30 days), DB credentials (7 days)
- No secrets in code, environment variables, or container images

### RBAC Model
| Role | Permissions |
|------|------------|
| `user` | View own engagements, manage preferences |
| `analyst` | Query intelligence API, export reports |
| `admin` | Manage users, feature flags, view audit logs |
| `super_admin` | Tenant management, model deployment, system config |

### Security Audit
- All admin actions logged with actor, action, target, timestamp
- Quarterly penetration testing
- Automated SAST/DAST in CI pipeline (Semgrep, OWASP ZAP)
