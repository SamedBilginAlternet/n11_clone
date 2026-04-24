#!/bin/sh
# Quick debug for a specific service.
# Usage: ./scripts/debug-service.sh n11-auth
#        ./scripts/debug-service.sh          (shows all crashed)

if [ -z "$1" ]; then
  echo "Usage: $0 <container-name>"
  echo ""
  echo "Available containers:"
  docker ps -a --filter "name=n11-" --format "  {{.Names}}  {{.Status}}" | sort
  echo ""
  echo "Crashed containers (last 20 lines each):"
  echo "─────────────────────────────────────────"
  for c in $(docker ps -a --filter "name=n11-" --filter "status=exited" --format "{{.Names}}"); do
    echo ""
    echo "=== $c ==="
    docker logs "$c" --tail 20 2>&1
  done
  for c in $(docker ps --filter "name=n11-" --filter "status=restarting" --format "{{.Names}}"); do
    echo ""
    echo "=== $c (restarting) ==="
    docker logs "$c" --tail 20 2>&1
  done
  exit 0
fi

container=$1
echo "=== $container ==="
echo ""

echo "── Status ──"
docker inspect "$container" --format '{{.State.Status}} (health: {{.State.Health.Status}}, restarts: {{.RestartCount}})' 2>/dev/null
echo ""

echo "── Last 30 log lines ──"
docker logs "$container" --tail 30 2>&1
echo ""

echo "── Memory ──"
docker stats "$container" --no-stream --format "{{.MemUsage}} ({{.MemPerc}})" 2>/dev/null
echo ""

echo "── Environment ──"
docker inspect "$container" --format '{{range .Config.Env}}{{println .}}{{end}}' 2>/dev/null | grep -E "(JAVA_TOOL|SPRING_DATASOURCE_URL|RABBITMQ|EUREKA|JWT_SECRET)" | sed 's/JWT_SECRET=.*/JWT_SECRET=***/'
