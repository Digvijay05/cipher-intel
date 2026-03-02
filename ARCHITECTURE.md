# CIPHER Platform Architecture

CIPHER (Conversational Intelligence Platform for Honeypot Engagement & Reporting) is a decentralized honeypot system for intercepting, engaging, and analyzing SMS scams in real-time.

It consists of two main layers:
1. **Frontend**: An autonomous Android application acting as the honeypot node.
2. **Backend**: A scalable FastAPI Python service that leverages LLMs to simulate a gullible target, analyze conversation context, and extract actionable threat intelligence.

## C4 Architecture Diagrams

### 1. Context Diagram

```mermaid
C4Context
    title System Context diagram for CIPHER

    Person(scammer, "Scammer", "A malicious actor sending phishing/scam SMS messages.")
    
    System(cipher, "CIPHER", "Autonomous SMS honeypot system that engages scammers to extract intelligence.")

    System_Ext(llmProvider, "LLM Provider", "Ollama Cloud, Groq, or OpenAI providing conversational AI.")
    System_Ext(reportingEndpoint, "Reporting API", "Configurable external endpoint where intelligence is delivered.")
    System_Ext(androidOS, "Android OS", "Telephony and SMS routing layer on the honeypot device.")

    Rel(scammer, androidOS, "Sends SMS scam texts", "SMS")
    Rel(androidOS, cipher, "Broadcasts SMS intents to app", "Intent")
    Rel(cipher, androidOS, "Sends replies via SmsManager", "SMS API")
    Rel(androidOS, scammer, "Delivers AI reply", "SMS")
    
    Rel(cipher, llmProvider, "Prompts AI for responses and extracts entities via JSON", "HTTPS/REST")
    Rel(cipher, reportingEndpoint, "Dispatches final scammer intelligence reports", "HTTPS/Webhook")
```

### 2. Container Diagram

```mermaid
C4Container
    title Container diagram for CIPHER

    Person(scammer, "Scammer", "Malicious actor")

    System_Boundary(c1, "CIPHER Frontend (Android Device)") {
        Container(androidApp, "Honeypot App", "Kotlin, Android", "Intercepts SMS, scores statically, forwards and replies. Manages Dual-SIM.")
        ContainerDb(roomDb, "Room Database", "SQLite", "Persists local app state, flags, and offline queues.")
    }

    System_Boundary(c2, "CIPHER Backend (Cloud)") {
        Container(fastApi, "API Application", "Python, FastAPI", "Orchestrates conversational sessions, scam analysis, and prompt injection.")
        ContainerDb(redis, "Session Cache", "Redis", "Holds short-term conversational context and session state across distributed instances.")
        ContainerDb(db, "Intelligence Store", "PostgreSQL/SQLite", "Stores long-term records of scammers, conversations, and intelligence entities.")
    }

    Rel(scammer, androidApp, "Sends SMS", "Cellular Network")
    Rel(androidApp, scammer, "Replies to SMS", "Cellular Network")
    
    Rel(androidApp, roomDb, "Reads/Writes offline state")
    
    Rel(androidApp, fastApi, "Submits new SMS text via REST API", "JSON/HTTPS")
    Rel(fastApi, redis, "Manages session state & history", "RESP")
    Rel(fastApi, db, "Persists final profiles & messages", "SQLAlchemy")

    System_Ext(llmCloud, "Cloud LLM (Ollama/Groq)", "External LLM endpoint")
    Rel(fastApi, llmCloud, "Generates conversational replies and extracts intelligence", "JSON/HTTPS")
```

### 3. Component Diagram (Android Frontend)

```mermaid
C4Component
    title Component diagram for CIPHER Android App

    Container_Boundary(androidApp, "Honeypot App") {
        Component(smsReceiver, "SmsReceiver", "BroadcastReceiver", "Intercepts PDU, parses text, and captures subscriptionId.")
        Component(smsDeliveryReceiver, "SmsDeliveryReceiver", "BroadcastReceiver", "Tracks OS-level sent/delivered status to log transmission failures.")
        Component(processingWorker, "SmsProcessingWorker", "WorkManager", "Statically scores messages for threat levels without network access.")
        Component(localValidator, "LocalValidator", "Kotlin Object", "Rule engine applying regex weights to detect urgency/financial keywords.")
        Component(engagementWorker, "EngagementWorker", "WorkManager", "Requests AI reply from backend and commands SmsManager to dispatch SMS to the precise SIM.")
        Component(retrofitClient, "RetrofitClient", "OkHttp/Retrofit", "Provides network transport to backend, featuring a fast-failing circuit breaker.")
    }

    Rel(smsReceiver, processingWorker, "Enqueues work with sender & SIM context")
    Rel(processingWorker, localValidator, "Scores threat text")
    Rel(processingWorker, engagementWorker, "Enqueues if score exceeds threshold")
    
    Rel(engagementWorker, retrofitClient, "Calls backend to generate reply")
    Rel(engagementWorker, smsDeliveryReceiver, "Attaches delivery PendingIntents before dispatch")

    System_Ext(backend, "CIPHER API Application", "FastAPI")
    Rel(retrofitClient, backend, "Sends payload", "HTTPS")
```

### 4. Component Diagram (Python Backend)

```mermaid
C4Component
    title Component diagram for CIPHER FastAPI Backend

    Container_Boundary(fastApi, "API Application") {
        Component(routes, "API Routes", "FastAPI Router", "Handles authenticated /api/honeypot/message calls.")
        Component(agentService, "AgentService", "Python Module", "Central orchestrator governing the Margaret persona loop.")
        Component(sessionService, "SessionService", "Python Module", "Loads and transitions state from memory or Redis.")
        Component(llmFactory, "LLMFactory", "Python Module", "Resolves context to optimal LLM provider (Ollama, Groq, or OpenAI).")
        Component(detectionService, "DetectionService", "Python Module", "Backup Regex detection layer applied heavily upon final reporting.")
        Component(extractionService, "ExtractionService", "Python Module", "Calls LLMs with strict structured outputs to rip phones, URLs, and UPI IDs.")
        Component(callbackService, "CallbackService", "Python Module", "Dispatches the unified Intelligence Report webhook upon session end.")
    }

    Rel(routes, agentService, "Passes message")
    Rel(agentService, sessionService, "Loads history")
    Rel(agentService, llmFactory, "Requests Persona reply given context")
    Rel(agentService, extractionService, "Requests entity extraction intermittently")
    Rel(agentService, sessionService, "Saves history")
    Rel(agentService, detectionService, "Fills final scammer profile scores")
    Rel(agentService, callbackService, "Triggers Webhook report")
```

## Security & Privacy Considerations

1. **Local-First Triage**: The Android frontend uses `LocalValidator` to triage incoming messages textually. Regular, non-scam SMS messages are dropped locally and **never** travel to the cloud API.
2. **Circuit Breaking**: The `RetrofitClient` avoids bombarding the LLM backend during provider outages. If the LLM goes down, the Android app safely idles instead of spinning up back-to-back threads.
3. **Multi-SIM Affinity**: Replies are firmly pinned to the exact `subscriptionId` routing the inbound intent. The AI won't mistakenly reply to a scam from your personal secondary SIM using your primary work SIM.
4. **Authentication**: Device-to-Cloud communication strictly employs `x-api-key`. 
5. **No Telemetry**: No third-party analytics are embedded in the APK.

## Future Milestones
- Next.js Web Dashboard for Visualizing active sessions and the extracted intelligence database.
- Enhanced context-compression for ultra-long scambaiting threads spanning > 20 turns.
- Webhook auto-retry capabilities for the `CallbackService`.
