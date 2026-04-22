#!/bin/bash
set -e

for db in authdb basketdb productdb orderdb paymentdb notificationdb reviewdb inventorydb; do
  echo "Creating database: $db"
  psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" -c "CREATE DATABASE $db;" || true
done
