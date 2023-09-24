#!/bin/sh
cd backend && mvn -f pom-first.xml install && cd .. && mvn -T4C clean install && git add infra/api-bucket/files
