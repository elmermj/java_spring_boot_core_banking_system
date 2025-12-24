#!/bin/bash

set -e

BASE_DIR="$HOME/Documents/experiments/java_spring_boot_core_banking_system"

SERVICES=(
  "api-gateway:8080"
  "transaction-orchestrator:8081"
  "ledger-service:8082"
  "account-service:8083"
  "audit-service:8084"
  "fx-service:8085"
  "scheduler-service:8086"
  "reconciliation-service:8087"
)

echo "=== Starting all JSB CBS services ==="
echo

for svc in "${SERVICES[@]}"; do
  IFS=":" read -r NAME PORT <<< "$svc"

  # Skip if already running
  if lsof -ti tcp:$PORT >/dev/null 2>&1; then
    echo "$NAME already running on port $PORT — SKIP"
    continue
  fi

  echo "Starting $NAME on port $PORT"
  cd "$BASE_DIR/$NAME"

  nohup ./mvnw spring-boot:run > logs.out 2>&1 &

  sleep 5
done

echo
echo "=== Waiting for services to warm up ==="
sleep 10

for svc in "${SERVICES[@]}"; do
  IFS=":" read -r NAME PORT <<< "$svc"
  echo -n "$NAME ($PORT): "
  curl -s --max-time 2 "http://localhost:$PORT/actuator/health" || echo "NOT RUNNING"
  echo
done

echo
echo "=== Start all complete ==="