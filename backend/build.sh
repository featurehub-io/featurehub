#!/bin/bash
# make sure you have built all artifacts first
if [ $# -eq 0 ]
  then
    VERSION=`cat ../current-rc.txt`
else
  VERSION=$1
fi
if [[ $VERSION == *"RC"* ]]; then
  echo "Is RC, not also tagging latest"
else
  echo "Is not RC, tagging latest"
  BUILD_PARAMS="$BUILD_PARAMS -Djib.to.tags=latest"
fi
DOCKER_PREFIX="${OVERRIDE_DOCKER_PREFIX:-featurehub}"
mvn -f pom-packages.xml -DskipTests $BUILD_PARAMS -Dapp.baseimage.prefix=docker:// -Ddocker.project.prefix=$DOCKER_PREFIX -Ddocker-cloud-build=true -Dbuild.version=$VERSION clean install && git add ../infra/api-bucket/files

