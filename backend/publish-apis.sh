#!/bin/bash
set -euxo pipefail
mvn -Dapp-release=true -f backend/messaging-api/pom.xml initialize
mvn -Dapp-release=true -f backend/edge-api/pom.xml initialize
mvn -Dapp-release=true -f backend/mr-api/pom.xml initialize
mvn -Dapp-release=true -f backend/enricher-api/pom.xml initialize
#find . -name 'pom.xml' | xargs grep -l maven-openapi-publisher | xargs -L 1 -I % sh -c "mvn -Dapp-release=true -f % initialize || exit 255"
