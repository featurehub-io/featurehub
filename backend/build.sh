#!/bin/sh
# make sure you have built all artifacts first
if [ $# -eq 0 ]
  then
    echo "No arguments supplied"
    exit -1
fi
DOCKER_PREFIX="${OVERRIDE_DOCKER_PREFIX:-featurehub}"
mvn -f pom-packages.xml -DskipTests $BUILD_PARAMS -Ddocker.project.prefix=$DOCKER_PREFIX -Ddocker-cloud-build=true -Dbuild.version=$1 clean install

