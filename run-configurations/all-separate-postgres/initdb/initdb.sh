#!/bin/bash
set -e

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
CREATE USER featurehub PASSWORD 'featurehub';
CREATE DATABASE featurehub;
GRANT ALL PRIVILEGES ON DATABASE featurehub TO featurehub;
EOSQL
