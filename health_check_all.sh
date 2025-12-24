#!/bin/bash

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

echo "=== Health Check All Services ==="
echo

for svc in "${SERVICES[@]}"; do
  IFS=":" read -r NAME PORT <<< "$svc"

  echo -n "$NAME ($PORT): "

  RESPONSE=$(curl -s --max-time 2 "http://localhost:$PORT/actuator/health")
  EXIT_CODE=$?

  if [ $EXIT_CODE -ne 0 ]; then
    echo "NOT RUNNING"
    continue
  fi

  STATUS=$(echo "$RESPONSE" | grep -o '"status":"[^"]*"' | head -1)

  if [[ "$STATUS" == *"UP"* ]]; then
    echo "UP"
  else
    echo "DOWN"
    echo "  → $RESPONSE"
  fi
done

echo
echo "=== Health check finished ==="