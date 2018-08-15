#!/usr/bin/env bash
if [ -f ../backend.conf ]; then
	BACKEND_CONF=../backend.conf
else
	BACKEND_CONF=./backend.conf
fi 
echo "Using $BACKEND_CONF"
java -jar application/target/application-1-SNAPSHOT.jar "$BACKEND_CONF" --dev application/src/main/resources/webapp/
