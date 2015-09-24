#!/bin/bash
psql -d brewingagile -U brewingagile -h localhost -c "COPY registrations FROM STDIN DELIMITER ','"

