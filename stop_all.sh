#!/bin/bash

set -e

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

echo "=== Stopping all JSB CBS services ==="

for svc in "${SERVICES[@]}"; do
  IFS=":" read -r NAME PORT <<< "$svc"
  PID=$(lsof -ti tcp:$PORT || true)

  if [ -n "$PID" ]; then
    echo "Stopping $NAME (PID $PID on port $PORT)"
    kill -9 $PID
  else
    echo "$NAME not running on port $PORT"
  fi
done

echo "=== All services stopped ==="