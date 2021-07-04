#!/bin/sh
if [ $# -eq 0 ]
  then
    echo "No arguments supplied"
    exit -1
fi
mvn -f pom-packages.xml -Ddocker-cloud-build=true -Dbuild.version=$1 -DskipTest clean install

