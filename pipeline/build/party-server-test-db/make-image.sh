#!/bin/sh
if [ $# -eq 0 ]
  then
    echo "Must specify the base party version then this image, e.g. ./make-image.sh 1.4.0 1.0.1"
    exit -1
fi

if [ $# -eq 1 ]
  then
    echo "Must specify the base party version then this image, e.g. ./make-image.sh 1.4.0 1.0.1"
    exit -1
fi
echo "FROM featurehub/party-server:$1" > Dockerfile
cat Dockerfile.part >> Dockerfile
docker build $DOCKER_OPTS --no-cache --squash -t featurehub/party-server-test-db:$2 .
