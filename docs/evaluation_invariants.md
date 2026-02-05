# Evaluation Invariants

## Goal
Eliminate ambiguity before writing code.

## Core Locks
* **Problem Statement**: **Problem Statement 2 (Agentic Honey-Pot)** is locked.
* **Final Callback**: This is a **HARD INVARIANT**. Missing the final callback results in a **ZERO SCORE**.

## Hard Rules
1. The system must produce a final callback to `https://hackathon.guvi.in/api/updateHoneyPotFinalResult`.
2. The callback must contain the exact extracted intelligence schema.
