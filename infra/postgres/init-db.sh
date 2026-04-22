#!/bin/bash
# Initializes per-service databases. Runs once, when the postgres volume is
# first created. Each microservice gets its own isolated database on the
# same Postgres instance — close enough to real "database-per-service" for
# local development without spinning up multiple Postgres containers.
set -e

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" <<-EOSQL
    CREATE DATABASE authdb;
    CREATE DATABASE basketdb;
    CREATE DATABASE productdb;
EOSQL
