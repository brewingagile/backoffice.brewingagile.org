#!/bin/bash
mvn clean package && java -jar application/target/application-1-SNAPSHOT.jar ./backend.conf --dev application/src/main/resources/webapp/
