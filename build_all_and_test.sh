#!/bin/sh
rm -rf ~/.m2/repository/io/featurehub
cd backend && mvn -f pom-first.xml install && cd .. && mvn -T4C clean install
