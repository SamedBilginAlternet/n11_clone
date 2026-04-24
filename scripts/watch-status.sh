#!/bin/sh
# Live container status monitor — shows health, restarts, and memory usage.
# Usage: ./scripts/watch-status.sh
# Exit: Ctrl+C

clear
echo "╔══════════════════════════════════════════════════════════════╗"
echo "║  n11 Clone — Container Status Monitor                      ║"
echo "║  Refreshing every 5s — Ctrl+C to stop                     ║"
echo "╚══════════════════════════════════════════════════════════════╝"
echo ""

while true; do
  tput cup 5 0 2>/dev/null || true
  echo "$(date '+%H:%M:%S') ────────────────────────────────────────────"
  echo ""
  docker ps -a --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}" \
    --filter "name=n11-" | sort
  echo ""

  # Count healthy/unhealthy/starting
  total=$(docker ps -a --filter "name=n11-" -q | wc -l)
  healthy=$(docker ps --filter "name=n11-" --filter "health=healthy" -q | wc -l)
  unhealthy=$(docker ps --filter "name=n11-" --filter "health=unhealthy" -q | wc -l)
  starting=$(docker ps --filter "name=n11-" --filter "health=starting" -q | wc -l)
  exited=$(docker ps -a --filter "name=n11-" --filter "status=exited" -q | wc -l)

  echo "Summary: ${healthy}/${total} healthy | ${starting} starting | ${unhealthy} unhealthy | ${exited} exited"
  echo ""

  # Show any crashed/restarting containers
  crashed=$(docker ps -a --filter "name=n11-" --filter "status=exited" --format "{{.Names}}" 2>/dev/null)
  restarting=$(docker ps --filter "name=n11-" --filter "status=restarting" --format "{{.Names}}" 2>/dev/null)

  if [ -n "$crashed" ]; then
    echo "⚠️  CRASHED:"
    for c in $crashed; do
      echo "  $c — last 3 lines:"
      docker logs "$c" --tail 3 2>&1 | sed 's/^/    /'
      echo ""
    done
  fi

  if [ -n "$restarting" ]; then
    echo "🔄 RESTARTING:"
    for c in $restarting; do
      echo "  $c — last 3 lines:"
      docker logs "$c" --tail 3 2>&1 | sed 's/^/    /'
      echo ""
    done
  fi

  # Show memory usage
  echo "Memory usage:"
  docker stats --no-stream --format "  {{.Name}}\t{{.MemUsage}}" \
    --filter "name=n11-" 2>/dev/null | sort

  sleep 5
done
