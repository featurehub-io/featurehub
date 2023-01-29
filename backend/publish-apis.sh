#!/bin/bash
cd ..
set -euxo pipefail
find . -name 'pom.xml' | xargs grep -l maven-openapi-publisher | xargs -L 1 -I % sh -c "mvn -Dapp-release=true -f % initialize || exit 255"
