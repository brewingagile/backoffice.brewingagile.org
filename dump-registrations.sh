#!/bin/bash
ssh postgres@linode2  "psql -d brewingagile -c \"COPY (SELECT badge, ticket, participant_name, dietary_requirements FROM registration ORDER BY badge DESC, ticket, participant_name) TO stdout WITH CSV\""
