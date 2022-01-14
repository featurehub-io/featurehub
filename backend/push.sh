#!/bin/zsh
if [ $# -eq 0 ]
  then
    VERSION=`cat ../current-rc.txt`
else
  VERSION=$VERSION
fi
DOCKER_PREFIX="${OVERRIDE_DOCKER_PREFIX:-featurehub}"
echo pushing to dockerhub version $VERSION

docker push $DOCKER_PREFIX/party-server:$VERSION
docker push $DOCKER_PREFIX/dacha:$VERSION
docker push $DOCKER_PREFIX/mr:$VERSION
docker push $DOCKER_PREFIX/edge:$VERSION
docker push $DOCKER_PREFIX/party-server-ish:$VERSION
docker push $DOCKER_PREFIX/edge-rest:$VERSION
if [[ $VERSION != *"RC"* ]]; then
  echo "not RC version, tagging latest and pushing to dockerhub"
  docker tag $DOCKER_PREFIX/party-server:$VERSION $DOCKER_PREFIX/party-server:latest
  docker tag $DOCKER_PREFIX/dacha:$VERSION $DOCKER_PREFIX/dacha:latest
  docker tag $DOCKER_PREFIX/mr:$VERSION $DOCKER_PREFIX/mr:latest
  docker tag $DOCKER_PREFIX/edge:$VERSION $DOCKER_PREFIX/edge:latest
  docker tag $DOCKER_PREFIX/party-server-ish:$VERSION $DOCKER_PREFIX/party-server-ish:latest
  docker tag $DOCKER_PREFIX/edge-rest:$VERSION $DOCKER_PREFIX/edge-rest:latest
  docker push $DOCKER_PREFIX/dacha:latest
  docker push $DOCKER_PREFIX/mr:latest
  docker push $DOCKER_PREFIX/edge:latest
  docker push $DOCKER_PREFIX/party-server:latest
  docker push $DOCKER_PREFIX/party-server-ish:latest
  docker push $DOCKER_PREFIX/edge-rest:latest
else
  echo "RC version, not tagging latest and pushing to dockerhub"
fi




