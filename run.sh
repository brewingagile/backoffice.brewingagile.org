#!/usr/bin/env bash
if [ -f ../backend.conf ]; then
	CONFIG=../backend.conf
	SECRET=../backend.conf
else
	CONFIG=./backend.conf
	SECRET=./backend.conf
fi 
echo "Using $CONFIG"
echo "Using $SECRET"
java -jar application/build/libs/application-all.jar --config-file=$CONFIG --secret-file=$SECRET --dev=application/src/main/resources/webapp/
