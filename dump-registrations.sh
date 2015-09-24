#!/bin/bash
ssh postgres@linode2  "psql -d brewingagile -c \"COPY registrations TO stdout DELIMITER ','\""