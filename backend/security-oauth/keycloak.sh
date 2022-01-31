#!/bin/sh
cd "${0%/*}"
docker run -p 8900:8080 -e DB_VENDOR=h2 -e KEYCLOAK_USER=admin -e KEYCLOAK_PASSWORD=admin -v $PWD/keycloak-db:/opt/jboss/keycloak/standalone/data quay.io/keycloak/keycloak:15.0.2
