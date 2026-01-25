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
[ API Gateway ]  ← JWT validation, rate limiting, routing, security
↓
[ Auth Service ]  ← authentication, JWT, OTP
↓
[ Transaction Orchestrator ]  ← single source of truth
↓
┌──────────────┬──────────────┬──────────────┐
│ Account Svc  │ Ledger Svc   │ FX Service   │
│ (balances)   │ (journal)    │ (rate lock)  │
└──────────────┴──────────────┴──────────────┘
↓
┌──────────────┬──────────────┬──────────────┐
│ Scheduler    │ Reconcile    │ Notification │
│ Service      │ Service      │ Service      │
│              │              │ (FCM/APNS)   │
└──────────────┴──────────────┴──────────────┘
↓
[ Audit & Reporting ]

---

## Core Services Overview

| Service | Responsibility | Port |
|------|----------------|------|
| API Gateway | Request routing, JWT validation, rate limiting, security, logging | 8080 |
| Auth Service | Authentication, JWT, OTP, device session management | 8088 |
| Notification Service | Push notifications (FCM/APNS), device token management | 8089 |
| Transaction Orchestrator | Global transaction state machine, idempotency, orchestration | 8081 |
| Account Service | Balance management, home data, debit/credit | 8083 |
| Ledger Service | Immutable financial journal | 8082 |
| FX Service | FX rate locking and conversion | 8085 |
| Scheduler Service | Scheduled and recurring payment execution | 8086 |
| Reconciliation Service | Finalizing pending or ambiguous transactions | 8087 |
| Audit Service | Compliance-grade audit logging | 8084 |

---

## Technology Stack

- Java 17+
- Spring Boot 4.0.1
- PostgreSQL 16 (unified database with schema separation)
- Flyway (database migrations)
- Redis (session management, rate limiting, caching)
- JWT (authentication tokens)
- BCrypt (password hashing)
- Firebase Cloud Messaging (FCM) - Android push notifications
- Apple Push Notification Service (APNS) - iOS push notifications
- Kafka (audit events, async processing)
- Prometheus & Grafana
- OpenTelemetry
- Docker / Kubernetes

---

## API Gateway Features

The API Gateway serves as the single entry point for all client requests and provides:

### Core Functions
- **Request Routing** - Routes requests to appropriate microservices based on path patterns
- **JWT Token Validation** - Validates JWT tokens before allowing access to secured endpoints
- **Rate Limiting** - Redis-based rate limiting (default: 20 requests/minute per account/IP)
- **Request/Response Logging** - Comprehensive logging of all API requests with timing
- **CORS Configuration** - Cross-origin resource sharing support
- **Security Headers** - Validates X-Account-Id header for secured routes
- **Account ID Verification** - Ensures X-Account-Id matches JWT token when both present

### Request Flow
1. **Request Logging** - Logs incoming request details
2. **Rate Limiting** - Checks if request is within rate limits
3. **Authentication** - Validates JWT token (for secured endpoints)
4. **Account ID Validation** - Validates X-Account-Id header format and matches with token
5. **Routing** - Routes to appropriate microservice

### Public Endpoints
- `/api/auth/**` - Authentication endpoints (no JWT required)
- `/actuator/**` - Health check and metrics

### Secured Endpoints
- `/api/account/**` - Requires JWT + X-Account-Id header
- `/api/transactions/**` - Requires JWT + X-Account-Id header
- `/api/notifications/**` - Internal service (called by other services)

## Authentication & Security Features

### Authentication
- **JWT-based authentication** with 24-hour token expiration
- **BCrypt password encryption** (industry-standard hashing)
- **Duplicate login prevention** - configurable per account
- **Multi-device support** - mobile and web sessions
- **OTP verification** for security-sensitive operations

### Device Session Management
- **MOBILE sessions** - single active session (if duplicate not allowed)
- **WEB sessions** - multiple sessions allowed (always requires OTP)
- **Session termination notifications** - real-time push to logged-out devices

### Push Notifications
- **FCM** (Firebase Cloud Messaging) for Android
- **APNS** (Apple Push Notification Service) for iOS
- **Auto-registration** - push tokens registered automatically during login
- **Idempotent operations** - safe to retry without duplicates

### Security Headers
- **X-Account-Id** header required for all secured endpoints
- Account context validation on every request

## Database Architecture

**Unified Database:** `jsb_cbs_unified`

All microservices use a single PostgreSQL database with **schema-based separation**:

| Service | Schema | Tables |
|---------|--------|--------|
| auth-service | auth_service | users, device_sessions, otp_requests |
| notification-service | notification_service | device_push_tokens |
| account-service | account_service | accounts, transactions, monthly_summary |
| ledger-service | ledger_service | accounts, transactions, ledger_entries |
| Other services | respective schemas | service-specific tables |

Benefits:
- Simplified backup and recovery
- Easier monitoring and management
- Logical isolation via schemas
- Independent Flyway migrations per service

## Project Status

🚧 **Work in Progress**

This repository is being developed incrementally, with a strong emphasis on:
- architectural correctness,
- failure handling,
- security best practices (OWASP compliance),
- and production readiness principles.

---

## License

MIT License
