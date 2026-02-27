# System Invariants

## Goal
Eliminate ambiguity before writing code.

## Core Locks
* **Architecture**: Agentic honeypot with multi-turn conversation engagement is locked.
* **Final Callback**: The system must produce a final intelligence report via the configured callback endpoint.

## Hard Rules
1. The system must produce a final callback to the configured `CIPHER_CALLBACK_URL`.
2. The callback must contain the exact extracted intelligence schema.
