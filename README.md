# java_spring_boot_core_banking_system
Robust core banking system with Java Spring Boot.

# Disclaimer
This project is a conceptual core banking system built for learning and architectural demonstration purposes.
It does not represent a complete or production-ready banking product.


# Purpose
- This system handles secured, consistent, and fault-proof internal transfers of funds with eventual consistency approach ala core banking system.
- This system is designed to prevent double transaction, handle delays between services, and is able to automatically recovered without manual intervention.

# Scope of functions
- Internal transfer (account to account)
- Backend-based idempotency
- Delay/timeout between services
- Automatic reconciliation
- Base rate limiter
- Audit log
- FX/currency conversion
- Scheduled/recurring payment

## Scope of functions not including:
- Inter-bank transfers
- External settlement
- Chargeback

# SLA & TIME BOUNDARY
- Idempotency deduplication window = 5 seconds
- Recon interval = 30 seconds
- Maximum pending before alert = 2 minutes
- Rate limit default = 20 requests / minute / user
- Retry from client = allowed, with same intent

# Hard rules
- Front-end must never determine the idempotency key
- No distributed lock
- No cross-service DB lock (for now)
- Recon must not re-execute debit/credit process
- Back-end emits canonical transaction ID
- All service are idempotent by transaction ID

# Failure philosophy
- System is assummed to be proned to failure: 
  1. Network delay
  2. Service crash
  3. Duplicated requests
  4. Partial execution
- System will not try to prevent failure, but will make sure the failures can be recovered safely.
