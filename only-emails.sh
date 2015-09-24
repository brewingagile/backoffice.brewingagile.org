#!/bin/bash
ssh postgres@linode2  "psql -d brewingagile -c \"COPY (select participant_email FROM registrations) TO stdout DELIMITER ','\""
