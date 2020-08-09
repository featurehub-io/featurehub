#!/bin/sh
if [ $# -eq 0 ]
  then
    echo "No arguments supplied"
    exit -1
fi
mvn -Ddocker-cloud-build=true -Dbuild.version=$1 -f pom-packages.xml clean install

docker run -p 8085:80 -v ~/tmp/party:/db us.gcr.io/demohub-283022/demohub/party-server:0.0.7
