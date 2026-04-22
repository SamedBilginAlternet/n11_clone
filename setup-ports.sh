#!/usr/bin/env bash
# Finds available ports and writes them to .env so docker-compose avoids conflicts.
set -euo pipefail

find_free_port() {
  local preferred=$1
  # Check if preferred port is free
  if ! ss -tlnH "sport = :$preferred" 2>/dev/null | grep -q ":$preferred" && \
     ! docker ps --format '{{.Ports}}' 2>/dev/null | grep -q "0.0.0.0:$preferred->"; then
    echo "$preferred"
    return
  fi
  # Otherwise grab a random free port from the OS
  python3 -c "import socket; s=socket.socket(); s.bind(('',0)); print(s.getsockname()[1]); s.close()"
}

echo "Discovering available ports..."

POSTGRES_PORT=$(find_free_port 5432)
RABBITMQ_PORT=$(find_free_port 5672)
RABBITMQ_MGMT_PORT=$(find_free_port 15672)
JAEGER_UI_PORT=$(find_free_port 16686)
JAEGER_GRPC_PORT=$(find_free_port 4317)
JAEGER_HTTP_PORT=$(find_free_port 4318)
LOKI_PORT=$(find_free_port 3100)
PROMETHEUS_PORT=$(find_free_port 9090)
GRAFANA_PORT=$(find_free_port 3001)
ELASTICSEARCH_PORT=$(find_free_port 9200)
GATEWAY_PORT=$(find_free_port 8000)
FRONTEND_PORT=$(find_free_port 3000)

cat > .env <<EOF
POSTGRES_PORT=$POSTGRES_PORT
RABBITMQ_PORT=$RABBITMQ_PORT
RABBITMQ_MGMT_PORT=$RABBITMQ_MGMT_PORT
JAEGER_UI_PORT=$JAEGER_UI_PORT
JAEGER_GRPC_PORT=$JAEGER_GRPC_PORT
JAEGER_HTTP_PORT=$JAEGER_HTTP_PORT
LOKI_PORT=$LOKI_PORT
PROMETHEUS_PORT=$PROMETHEUS_PORT
GRAFANA_PORT=$GRAFANA_PORT
ELASTICSEARCH_PORT=$ELASTICSEARCH_PORT
GATEWAY_PORT=$GATEWAY_PORT
FRONTEND_PORT=$FRONTEND_PORT
EOF

echo ""
echo "Port allocations written to .env:"
echo "  PostgreSQL       : $POSTGRES_PORT"
echo "  RabbitMQ AMQP    : $RABBITMQ_PORT"
echo "  RabbitMQ Mgmt    : $RABBITMQ_MGMT_PORT"
echo "  Jaeger UI        : $JAEGER_UI_PORT"
echo "  Jaeger gRPC      : $JAEGER_GRPC_PORT"
echo "  Jaeger HTTP      : $JAEGER_HTTP_PORT"
echo "  Loki             : $LOKI_PORT"
echo "  Prometheus       : $PROMETHEUS_PORT"
echo "  Grafana          : $GRAFANA_PORT"
echo "  Elasticsearch    : $ELASTICSEARCH_PORT"
echo "  API Gateway      : $GATEWAY_PORT"
echo "  Frontend         : $FRONTEND_PORT"
echo ""
echo "Run: docker compose up -d"
