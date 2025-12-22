# java_spring_boot_core_banking_system

A robust, microservice-based financial transaction platform built with Java Spring Boot, designed with core banking–style architecture principles.

---

## Disclaimer

This project is a **conceptual financial transaction system** built for learning, experimentation, and architectural demonstration purposes.

It **does not represent a complete or production-ready core banking product**, nor is it affiliated with or intended to replicate any real-world banking system.

---

## Purpose

This system implements a **distributed financial transaction platform** using a **microservice architecture**.

It is designed to:
- handle internal monetary transactions safely,
- tolerate partial failures and inter-service delays,
- prevent duplicate or inconsistent transactions,
- and recover automatically without manual intervention.

The system adopts an **eventual consistency model**, inspired by modern core banking and large-scale financial platforms.

---

## Architectural Principles

- **Single Source of Truth** via a Transaction Orchestrator
- **Eventual Consistency**, not distributed transactions
- **Backend-controlled idempotency**
- **Failure-tolerant by design**
- **Recoverability over immediacy**
- **Auditability and traceability first**

---

## Scope of Functions

### In Scope
- Internal account-to-account transfers
- Backend-generated canonical transaction IDs
- FX rate locking and currency conversion
- Scheduled and recurring payment execution
- Automatic reconciliation across services
- Rate limiting and abuse protection
- Immutable, append-only audit logging
- Delay and timeout handling between services

---

### Out of Scope
- Inter-bank settlement networks (RTGS, SWIFT, BI-FAST, etc.)
- External clearing houses
- Chargeback and dispute resolution
- Interest calculation, loans, or credit products
- Regulatory reporting integrations

---

## Consistency Model

**Eventual Consistency**

The system explicitly avoids:
- Distributed transactions (2PC / XA)
- Cross-service database joins
- Cross-service database locks

Transactions may enter intermediate states (e.g. `PENDING`) and are finalized asynchronously through reconciliation processes.

---

## SLA & Time Boundaries

| Aspect | Value |
|------|------|
| Idempotency deduplication window | 5 seconds |
| Primary reconciliation interval | 30 seconds |
| Maximum pending duration before alert | 2 minutes |
| Default rate limit | 20 requests / minute / user |
| Client retries | Allowed, with the same business intent |

---

## Hard Rules (Non-Negotiable)

- Frontend **must never** determine idempotency keys or transaction identity
- All monetary actions must be routed through the **Transaction Orchestrator**
- FX rates must be **locked and referenced by rate ID**
- Scheduler emits **execution intents**, not transactions
- No distributed database transactions or locks
- No blind retries for monetary actions
- All services must be **idempotent by transaction ID**
- Reconciliation must **never re-execute debit or credit operations**

---

## Failure Philosophy

The system is assumed to be **inherently failure-prone** due to:
1. Network delays
2. Service crashes
3. Duplicate or replayed requests
4. Partial or ambiguous execution

Failures are treated as **expected system states**, not exceptional cases.

The system does not attempt to prevent all failures; instead, it ensures that **all failures are safely recoverable** through deterministic state transitions and reconciliation mechanisms.

---

## High-Level Architecture
[ Client ]
↓
[ WAF / API Gateway ]
↓
[ Authentication & Authorization ]
↓
[ Transaction Orchestrator ]  ← single source of truth
↓
┌──────────────┬──────────────┬──────────────┐
│ Account Svc  │ Ledger Svc   │ FX Service   │
│ (balances)   │ (journal)    │ (rate lock)  │
└──────────────┴──────────────┴──────────────┘
↓
[ Scheduler Service ]
↓
[ Reconciliation Engine ]
↓
[ Audit & Reporting ]

---

## Core Services Overview

| Service | Responsibility |
|------|----------------|
| Transaction Orchestrator | Global transaction state machine, idempotency, orchestration |
| Account Service | Balance management and debit/credit |
| Ledger Service | Immutable financial journal |
| FX Service | FX rate locking and conversion |
| Scheduler Service | Scheduled and recurring payment execution |
| Reconciliation Service | Finalizing pending or ambiguous transactions |
| Audit Service | Compliance-grade audit logging |

---

## Technology Stack (Planned)

- Java 17+
- Spring Boot
- PostgreSQL
- Flyway
- Redis (rate limiting / caching)
- Kafka (audit events, async processing)
- Prometheus & Grafana
- OpenTelemetry
- Docker / Kubernetes

---

## Project Status

🚧 **Work in Progress**

This repository is being developed incrementally, with a strong emphasis on:
- architectural correctness,
- failure handling,
- and production readiness principles.

---

## License

MIT License
