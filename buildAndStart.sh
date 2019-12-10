#!/usr/bin/env bash
echo "Make sure you have PostgreSQL running somewhere, "
echo "and make sure 'backend.conf' is pointing to that database."
./gradlew clean build && ./run.sh
