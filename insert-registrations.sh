#!/usr/bin/env bash
DB_HOST="${DB_HOST:-localhost}"
echo "Using PostgreSQL at $DB_HOST"
psql -d brewingagile -U brewingagile -h $DB_HOST -c "COPY registrations FROM STDIN DELIMITER ','"

