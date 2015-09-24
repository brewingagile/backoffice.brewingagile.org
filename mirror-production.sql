#!/bin/bash
set -e
PSQL="psql -U brewingagile -h localhost brewingagile"
PSQLI="$PSQL -t -c"

### Drop all tables
DROPSTATEMENTS=`$PSQLI "select 'drop table if exists \"' || tablename || '\" cascade;' from pg_tables where schemaname = 'public';"`
echo $DROPSTATEMENTS | while read -r line; do $PSQLI "$line"; done

ssh postgres@linode2 pg_dump -F p brewingagile | $PSQL -a -f -

#$PSQLI "INSERT INTO users(username, passwordhash, role, role_co) VALUES ('admin', 'dbf39a8cfdeb4191b678fdb0d86896fe499f4e48', 'ADMIN', NULL);"
