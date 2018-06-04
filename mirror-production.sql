#!/usr/bin/env bash
set -e

DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-5433}"
echo "Using PostgreSQL at $DB_HOST:$DB_PORT"

PSQL="psql -U brewingagile --host=$DB_HOST --port=$DB_PORT brewingagile"
PSQLI="$PSQL -t -c"

### Drop all tables
DROPSTATEMENTS=`$PSQLI "select 'drop table if exists \"' || tablename || '\" cascade;' from pg_tables where schemaname = 'public';"`
echo $DROPSTATEMENTS | while read -r line; do $PSQLI "$line"; done

ssh postgres@linode2 pg_dump -F p brewingagile | $PSQL -a -f -

$PSQLI "UPDATE registration SET participant_email = 'henrik@sjostrand.at';"
$PSQLI "UPDATE account SET billing_email = 'henrik@sjostrand.at';"

#$PSQLI "INSERT INTO users(username, passwordhash, role, role_co) VALUES ('admin', 'dbf39a8cfdeb4191b678fdb0d86896fe499f4e48', 'ADMIN', NULL);"

