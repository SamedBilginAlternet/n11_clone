#!/bin/sh
# Waits until all containers are healthy, then prints URLs.
# Usage: ./scripts/wait-ready.sh
# Timeout: 5 minutes

echo "⏳ Waiting for all services to become healthy..."
echo ""

TIMEOUT=300
ELAPSED=0

while [ $ELAPSED -lt $TIMEOUT ]; do
  total=$(docker ps -a --filter "name=n11-" -q | wc -l)
  healthy=$(docker ps --filter "name=n11-" --filter "health=healthy" -q | wc -l)
  exited=$(docker ps -a --filter "name=n11-" --filter "status=exited" -q | wc -l)

  # Frontend has no healthcheck — count it separately
  frontend_up=$(docker ps --filter "name=n11-frontend" --filter "status=running" -q | wc -l)

  ready=$((healthy + frontend_up))

  printf "\r  %d/%d ready (%ds elapsed)" "$ready" "$total" "$ELAPSED"

  if [ "$exited" -gt 0 ]; then
    echo ""
    echo ""
    echo "❌ Some containers crashed:"
    docker ps -a --filter "name=n11-" --filter "status=exited" --format "  {{.Names}} — {{.Status}}"
    echo ""
    echo "Run: ./scripts/debug-service.sh  (for details)"
    exit 1
  fi

  if [ "$ready" -ge "$total" ] && [ "$total" -gt 0 ]; then
    echo ""
    echo ""
    echo "✅ All $total containers are ready!"
    echo ""
    echo "═══════════════════════════════════════════════════"
    echo "  Frontend:    http://localhost:13000"
    echo "  Gateway:     http://localhost:18000"
    echo "  Eureka:      http://localhost:18761"
    echo "  Grafana:     http://localhost:13001  (admin/admin)"
    echo "  Jaeger:      http://localhost:26686"
    echo "  RabbitMQ:    http://localhost:25672  (guest/guest)"
    echo "  Prometheus:  http://localhost:19090"
    echo "  ES:          http://localhost:19200"
    echo "═══════════════════════════════════════════════════"
    echo ""
    echo "Demo accounts:"
    echo "  user@n11demo.com / User123!"
    echo "  admin@n11demo.com / Admin123!"
    echo "  failuser@n11demo.com / User123!  (payment fails)"
    exit 0
  fi

  sleep 5
  ELAPSED=$((ELAPSED + 5))
done

echo ""
echo ""
echo "⏰ Timeout after ${TIMEOUT}s. Not all services are ready."
echo ""
echo "Current status:"
docker ps -a --filter "name=n11-" --format "  {{.Names}}  {{.Status}}" | sort
echo ""
echo "Run: ./scripts/debug-service.sh  (for crash details)"
exit 1
