#!/bin/bash
ssh postgres@linode2  "psql -d brewingagile -c \"COPY (select participant_name, ticket, dietary_requirements FROM registration WHERE dietary_requirements <> '') TO stdout WITH CSV\""
