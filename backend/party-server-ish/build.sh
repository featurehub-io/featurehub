#!/bin/sh
# make sure you have built all artifacts first
if [ $# -eq 0 ]
  then
    echo "No arguments supplied"
    exit -1
fi
mvn  -DskipTests $BUILD_PARAMS -Ddocker-cloud-build=true -Dbuild.version=$1 clean install

