#!/bin/sh
if [ $# -eq 0 ]
  then
    echo "No arguments supplied"
    exit -1
fi
cd backend
mvn -Ddocker-cloud-build=true -Dbuild.version=$1 -f pom-packages.xml clean install
