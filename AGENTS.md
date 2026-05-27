# AGENTS.md

## Cursor Cloud specific instructions

### Architecture

Microservice-based core banking system (10 Spring Boot 4.0.1 services, Java 17+). Each service is independent with its own Maven wrapper (`./mvnw`). No root parent POM. All services share a single PostgreSQL 16 database (`jsb_cbs_unified`) with schema-based separation.

For service details, ports, and API routes see `README.md`.

### Infrastructure (must start before services)

1. **PostgreSQL 16**: `docker compose -f docker/postgres.yml up -d` — creates DB `jsb_cbs_unified` with 9 schemas via `docker/init-db.sql`.
2. **Redis**: `docker run -d --name redis-server -p 6379:6379 redis:7-alpine` — required by api-gateway (rate limiting), auth-service, and transaction-orchestrator.

### Starting services

Each service runs via `./mvnw spring-boot:run` from its own directory. Start order matters for inter-service calls:

1. `auth-service` (8088), `notification-service` (8089) — auth calls notification on login
2. `account-service` (8083), `ledger-service` (8082), `fx-service` (8085), `audit-service` (8084)
3. `transaction-orchestrator` (8081)
4. `api-gateway` (8080) — see known issues below

Health check: `curl http://localhost:<port>/actuator/health`

### Known pre-existing issues

- **api-gateway (8080)**: Fails to start due to duplicate bean definition (`requestLoggingFilter` defined both as `@Component` in `RequestLoggingFilter.java` and via `@Bean` in `FilterConfig.java`). Setting `SPRING_MAIN_ALLOW_BEAN_DEFINITION_OVERRIDING=true` partially fixes it but reveals a secondary `AccountIdValidationFilter` injection issue. Bypass by calling downstream services directly during development.
- **account-service**: The Flyway migration `V1__init_account_schema.sql` defines `currency CHAR(3)` but the JPA entity expects `VARCHAR(3)`. After Flyway runs, fix with: `ALTER TABLE account_service.accounts ALTER COLUMN currency TYPE VARCHAR(3);`
- **scheduler-service / reconciliation-service**: Fail to start because Quartz tables (`qrtz_locks` etc.) don't exist. Config has `initialize-schema: never` but no migration creates them.
- **Kafka**: Declared in config for 7 services but no Kafka producers/consumers are implemented yet. Services start without Kafka but may log connection warnings.

### Running tests

Each service has `@SpringBootTest` context-loading tests. Run with `./mvnw test` per service. Tests that pass with infrastructure up: auth-service, notification-service, transaction-orchestrator, ledger-service, fx-service, audit-service. Tests that fail due to pre-existing code issues: api-gateway, account-service, scheduler-service, reconciliation-service.

### auth-service has no registration endpoint

Users must be seeded directly in the database. Example:
```sql
INSERT INTO auth_service.users (id, account_number, password_hash, is_duplicate_allowed, is_balance_hidden, created_at, updated_at)
VALUES (gen_random_uuid(), '1234567890', '<bcrypt_hash>', true, false, now(), now());
```
Generate BCrypt hash with: `python3 -c "import bcrypt; print(bcrypt.hashpw(b'password123', bcrypt.gensalt()).decode())"`

### auth-service `.mvn` directory structure

The `auth-service/.mvn/wrapper/maven-wrapper.properties` file is nested incorrectly at `.mvn/.mvn/wrapper/`. Copy it to the correct location before building:
```
mkdir -p auth-service/.mvn/wrapper
cp auth-service/.mvn/.mvn/wrapper/maven-wrapper.properties auth-service/.mvn/wrapper/
```
