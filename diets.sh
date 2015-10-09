#!/bin/bash
ssh postgres@linode2  "psql -d brewingagile -c \"COPY (select participant_name, dietary_requirements FROM registrations WHERE dietary_requirements <> '') TO stdout DELIMITER ','\""
