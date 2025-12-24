#!/bin/bash

set -e

BASE_DIR="$HOME/Documents/experiments/java_spring_boot_core_banking_system"

SERVICES=(
  "api-gateway:8080"
  "transaction-orchesh strator:8081"
  "ledger-service:8082"
  "account-service:8083"
  "audit-service:8084"
  "fx-service:8085"
  "scheduler-service:8086"
  "reconciliation-service:8087"
)

echo "=== Stopping running services ==="

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

sleep 2

echo
echo "=== Starting services ==="

for svc in "${SERVICES[@]}"; do
  IFS=":" read -r NAME PORT <<< "$svc"
  echo "Starting $NAME on port $PORT"

  cd "$BASE_DIR/$NAME"
  nohup ./mvnw spring-boot:run > logs.out 2>&1 &

  sleep 5
done

echo
echo "=== Waiting for health checks ==="
sleep 10

for svc in "${SERVICES[@]}"; do
  IFS=":" read -r NAME PORT <<< "$svc"
  echo -n "$NAME : "
  curl -s "http://localhost:$PORT/actuator/health" || echo "DOWN"
  echo
done

echo
echo "=== Restart complete ==="