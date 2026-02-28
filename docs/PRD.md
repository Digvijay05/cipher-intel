# CIPHER — Product Requirements Document

## 1. Product Goals

### Year 1 (Foundation)
- Launch Android app with on-device scam detection and autonomous engagement
- Establish backend intelligence extraction pipeline
- Build initial intelligence graph with 50K+ unique scam infrastructure nodes
- Achieve 100K installs with 5% premium conversion
- Ship B2B intelligence API (beta) to 3 design partners (1 bank, 1 fintech, 1 telco)

### Year 3 (Scale)
- Cross-platform (iOS, Android, browser extension)
- 10M+ app installs across India, SEA, Africa
- Intelligence graph: 5M+ nodes with real-time updates
- Enterprise API serving 50+ B2B customers
- Predictive scam pattern detection (new campaign identification within hours of launch)
- Self-learning engagement models (fine-tuned on proprietary conversation corpus)
- Regulatory partnership with at least 2 national cybercrime agencies

---

## 2. User Personas

### Persona 1: Ramesh (Consumer — Primary)
- 58-year-old retired bank manager, Tier-2 city India
- First-generation smartphone user, uses UPI daily
- Receives 3-5 scam SMS/calls per week
- **Need**: Protection without requiring technical knowledge
- **Behavior**: Won't install complex security apps, needs zero-config protection

### Persona 2: Priya (Consumer — Tech-Savvy)  
- 28-year-old software engineer, metro city
- Aware of scams but wants to actively fight back
- **Need**: Visibility into scam operations, satisfaction of wasting scammer time
- **Behavior**: Will enable all features, review engagement transcripts, share intel

### Persona 3: Vikram (B2B — Banking CISO)
- Chief Information Security Officer at a mid-size Indian bank
- Responsible for fraud prevention and RBI compliance
- **Need**: Real-time feed of active scam UPI endpoints and phishing domains to block preemptively
- **Behavior**: Consumes API, integrates into existing SIEM/fraud detection stack

### Persona 4: Telecom Fraud Analyst
- Works at carrier fraud ops team
- **Need**: Identify scam-originating numbers + SIM farms before complaints arrive
- **Behavior**: Batch intelligence queries, automated blocklist updates

---

## 3. Core Features

### MVP (Month 1-6)

| Feature | Description | Priority |
|---------|-------------|----------|
| SMS Scam Detection | On-device heuristic + backend ML classification | P0 |
| Autonomous Engagement | Persona-driven AI agent auto-replies to confirmed scams | P0 |
| Intelligence Extraction | Regex + NLP extraction of UPI, phones, URLs, bank accounts | P0 |
| Engagement Dashboard | User views active/completed engagements with extracted intel | P0 |
| Threat Notifications | Real-time alerts for high-risk incoming messages | P0 |
| Kill Switch | User can disable all autonomous engagement instantly | P0 |
| Engagement History | Scrollable conversation transcripts with highlighted intelligence | P1 |
| Intelligence API (Beta) | REST API for B2B partners to query extracted intelligence | P1 |

### Scale (Month 6-18)

| Feature | Description | Priority |
|---------|-------------|----------|
| Intelligence Graph Explorer | Visual graph showing scam networks (linked numbers, UPIs, domains) | P1 |
| Multi-Channel Support | WhatsApp, Telegram, voice call detection | P1 |
| Predictive Alerts | ML model predicts likely scam contact before it happens | P2 |
| Community Intelligence | Anonymized threat sharing across user base | P1 |
| Enterprise Dashboard | Multi-seat admin console with RBAC, audit logs, SLA tracking | P1 |
| Custom Personas | B2B customers can configure engagement persona behavior | P2 |
| Offline Mode | Full detection capability without network connectivity | P2 |

---

## 4. AI-Driven Engagement Capabilities

### Persona Engine
- Multiple pre-built personas (elderly retiree, confused student, busy professional)
- Each persona has: age, background, vocabulary patterns, technical literacy level, emotional responses
- Persona selection is automatic based on scam type (financial → elderly persona, tech support → non-technical persona)

### Conversation Strategy
- **Phase 1 (Hook)**: Express alarm, show compliance willingness
- **Phase 2 (Build Trust)**: Ask clarifying questions, delay with plausible excuses
- **Phase 3 (Extract)**: Attempt to follow instructions, bait for payment/credential details
- **Phase 4 (Sustain)**: Invent technical difficulties, extend conversation
- **Phase 5 (Disengage)**: Introduce third party ("my grandson is here"), graceful exit

### Self-Reflection Loop
- LLM output is validated against a structured JSON schema
- Reflection evaluator rejects responses that break character, reveal AI nature, or produce harmful content
- Failed responses trigger retry with temperature escalation (up to 3 attempts)

### Memory Architecture
- Per-session conversation history with sliding window compression
- Cross-session scammer profiling (if same number contacts multiple users)
- Missing-entity tracking: system knows which intelligence types haven't been extracted yet, adjusts strategy accordingly

---

## 5. Intelligence Graph System

### Node Types
| Type | Example | Source |
|------|---------|--------|
| Phone Number | +919876543210 | Incoming SMS sender, extracted from conversation |
| UPI Endpoint | helpdesk.sbi@ybl | Extracted from conversation |
| Phishing Domain | sbi-verify.scam | Extracted from SMS body |
| Bank Account | 1234567890123456 | Extracted from conversation |
| Scam Campaign | "SBI KYC Suspension" | Clustered from similar messages |

### Edge Types
| Edge | Meaning |
|------|---------|
| SENT_FROM → | This phone number sent this scam message |
| REFERENCES → | This message contained this UPI/URL/account |
| BELONGS_TO → | This UPI endpoint is linked to this campaign |
| CO_OCCURS_WITH → | These entities appeared in the same conversation |
| TEMPORAL_CLUSTER → | These entities were active in the same time window |

### Intelligence Compounding
- Every engagement adds nodes and edges
- Graph queries enable: "Given this new UPI endpoint, what campaign is it linked to? What other endpoints does that campaign use?"
- Temporal analysis reveals campaign lifecycle: launch → peak → dormancy → mutation

---

## 6. Analytics & Reporting

### Consumer Dashboard
- Total scams intercepted (lifetime, 30d, 7d)
- Engagement status (active, completed, intelligence extracted)
- Top scam categories encountered
- Personal threat level trend

### Enterprise Dashboard  
- Real-time intelligence feed (new entities last 24h)
- Top active campaigns with entity counts
- Geographic scam heatmap
- API usage metrics and quota tracking
- Alert rules (notify when specific entity types appear)
- Exportable intelligence reports (CSV, JSON, STIX format)

---

## 7. Admin Control Surfaces

- **Feature flags**: Enable/disable engagement, detection, intelligence sharing per-user or globally
- **Model versioning**: A/B test detection models, rollback on regression
- **Persona management**: Create, edit, archive engagement personas
- **Rate limit configuration**: Per-user, per-session, global engagement limits
- **Compliance controls**: Data retention policies, right-to-delete, audit log access
- **Incident response**: Manual session termination, user-level engagement freeze

---

## 8. Security & Privacy Model

| Concern | Approach |
|---------|----------|
| User data at rest | AES-256 encryption, per-user key derivation |
| Data in transit | TLS 1.3 mandatory, certificate pinning on mobile |
| PII handling | Conversation content never leaves user's device by default. Only extracted entity data (UPIs, phone numbers) is transmitted to backend — with user consent |
| API authentication | API key + JWT token with short expiry, OAuth2 for enterprise SSO |
| Key management | Vault-based secret management, automated rotation |
| Access control | RBAC with principle of least privilege, audit logging on all admin actions |
| Data retention | Configurable per-tenant, default 90-day rolling window, hard delete on account deletion |

---

## 9. Compliance Readiness

- **GDPR-style data rights**: Right to access, rectify, delete, port
- **Indian IT Act compliance**: Lawful interception cooperation framework (with proper legal process)
- **Telecom regulatory alignment**: SMS processing compliant with TRAI consent framework
- **SOC 2 Type II**: Target certification by Year 2
- **Data Processing Agreements**: Standard DPA for all B2B customers

---

## 10. Failure Modes

| Failure | Impact | Mitigation |
|---------|--------|------------|
| LLM generates harmful content | Reputational damage, legal risk | Reflection evaluator + content guardrails + human review sampling |
| False positive (legitimate message engaged) | User trust erosion | Conservative detection threshold, user override, feedback loop |
| Scammer detects AI | Engagement terminated early, reduced intel yield | Persona realism scoring, human-like typing delays, varied response patterns |
| Backend outage | No engagement capability | Graceful degradation — detection continues, engagement queued for retry |
| Intelligence data poisoning | Fraudulent entities pollute graph | Cross-validation across multiple independent engagements, confidence scoring |

---

## 11. Success Metrics

| Metric | Year 1 Target | Definition |
|--------|---------------|------------|
| Scam Detection Precision | >95% | % of flagged messages that are actually scams |
| Engagement Completion Rate | >60% | % of engagements reaching 10+ turns |
| Intelligence Yield | >40% | % of engagements that extract at least 1 novel entity |
| Mean Scammer Time Wasted | >8 min | Average duration of engagement before scammer disengages |
| B2C MAU | 50K | Monthly active users on mobile app |
| B2B API Partners | 3 | Paying enterprise customers |
| Intelligence Graph Nodes | 50K | Unique scam infrastructure entities |

---

## 12. Growth Loops

### Loop 1: Detection → Trust → Referral
User receives scam → CIPHER detects it → User is protected → Tells friends → More installs

### Loop 2: Engagement → Intelligence → Better Detection
More engagements → More extracted entities → Detection model improves → Fewer false positives → More users enable engagement

### Loop 3: B2B Intelligence → Revenue → R&D → Better Product
Enterprise pays for intelligence → Revenue funds model training → Better models → More intelligence → More enterprise value

### Loop 4: Scammer Behavior Data → Predictive Models → Preemptive Protection
Conversation transcripts → Scammer tactic taxonomy → Predict new scam patterns → Protect users before scam reaches them
