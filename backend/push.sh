#!/bin/sh
if [ $# -eq 0 ]
  then
    echo "No arguments supplied"
    exit -1
fi
DOCKER_PREFIX="${OVERRIDE_DOCKER_PREFIX:-featurehub}"
docker tag $DOCKER_PREFIX/party-server:$1 $DOCKER_PREFIX/party-server:latest
docker tag $DOCKER_PREFIX/dacha:$1 $DOCKER_PREFIX/dacha:latest
docker tag $DOCKER_PREFIX/mr:$1 $DOCKER_PREFIX/mr:latest
docker tag $DOCKER_PREFIX/edge:$1 $DOCKER_PREFIX/edge:latest
docker tag $DOCKER_PREFIX/party-server-ish:$1 $DOCKER_PREFIX/party-server-ish:latest
docker tag $DOCKER_PREFIX/edge-rest:$1 $DOCKER_PREFIX/edge-rest:latest
docker push $DOCKER_PREFIX/party-server:$1
docker push $DOCKER_PREFIX/dacha:$1
docker push $DOCKER_PREFIX/mr:$1
docker push $DOCKER_PREFIX/edge:$1
docker push $DOCKER_PREFIX/party-server:latest
docker push $DOCKER_PREFIX/dacha:latest
docker push $DOCKER_PREFIX/mr:latest
docker push $DOCKER_PREFIX/edge:latest
docker push $DOCKER_PREFIX/party-server-ish:latest
docker push $DOCKER_PREFIX/party-server-ish:$1
docker push $DOCKER_PREFIX/edge-rest:$1
docker push $DOCKER_PREFIX/edge-rest:latest




