-- Create schemas for each microservice within the unified database
CREATE SCHEMA IF NOT EXISTS account_service;
CREATE SCHEMA IF NOT EXISTS ledger_service;
CREATE SCHEMA IF NOT EXISTS audit_service;
CREATE SCHEMA IF NOT EXISTS fx_service;
CREATE SCHEMA IF NOT EXISTS reconciliation_service;
CREATE SCHEMA IF NOT EXISTS scheduler_service;
CREATE SCHEMA IF NOT EXISTS transaction_orchestrator;
CREATE SCHEMA IF NOT EXISTS auth_service;
CREATE SCHEMA IF NOT EXISTS notification_service;

-- Grant privileges to the jsb user on all schemas
GRANT ALL PRIVILEGES ON SCHEMA account_service TO jsb;
GRANT ALL PRIVILEGES ON SCHEMA ledger_service TO jsb;
GRANT ALL PRIVILEGES ON SCHEMA audit_service TO jsb;
GRANT ALL PRIVILEGES ON SCHEMA fx_service TO jsb;
GRANT ALL PRIVILEGES ON SCHEMA reconciliation_service TO jsb;
GRANT ALL PRIVILEGES ON SCHEMA scheduler_service TO jsb;
GRANT ALL PRIVILEGES ON SCHEMA transaction_orchestrator TO jsb;
GRANT ALL PRIVILEGES ON SCHEMA auth_service TO jsb;
GRANT ALL PRIVILEGES ON SCHEMA notification_service TO jsb;

-- Set default privileges for future objects
ALTER DEFAULT PRIVILEGES IN SCHEMA account_service GRANT ALL ON TABLES TO jsb;
ALTER DEFAULT PRIVILEGES IN SCHEMA ledger_service GRANT ALL ON TABLES TO jsb;
ALTER DEFAULT PRIVILEGES IN SCHEMA audit_service GRANT ALL ON TABLES TO jsb;
ALTER DEFAULT PRIVILEGES IN SCHEMA fx_service GRANT ALL ON TABLES TO jsb;
ALTER DEFAULT PRIVILEGES IN SCHEMA reconciliation_service GRANT ALL ON TABLES TO jsb;
ALTER DEFAULT PRIVILEGES IN SCHEMA scheduler_service GRANT ALL ON TABLES TO jsb;
ALTER DEFAULT PRIVILEGES IN SCHEMA transaction_orchestrator GRANT ALL ON TABLES TO jsb;
ALTER DEFAULT PRIVILEGES IN SCHEMA auth_service GRANT ALL ON TABLES TO jsb;
ALTER DEFAULT PRIVILEGES IN SCHEMA notification_service GRANT ALL ON TABLES TO jsb;

