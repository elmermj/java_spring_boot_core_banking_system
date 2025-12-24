-- Create the orchestrator user and grant privileges
CREATE USER orchestrator WITH PASSWORD 'orchestrator_pwd';
GRANT ALL PRIVILEGES ON DATABASE orchestrator_db TO orchestrator;
ALTER DATABASE orchestrator_db OWNER TO orchestrator;

