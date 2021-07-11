#!/bin/sh
docker build $DOCKER_OPTS --no-cache --squash -t featurehub/party-sdk:1.3.4 .
