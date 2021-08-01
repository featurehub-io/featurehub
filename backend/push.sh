#!/bin/sh
if [ $# -eq 0 ]
  then
    echo "No arguments supplied"
    exit -1
fi
docker push featurehub/party-server:$1
docker push featurehub/dacha:$1
docker push featurehub/mr:$1
docker push featurehub/edge:$1
docker push featurehub/party-server:latest
docker push featurehub/dacha:latest
docker push featurehub/mr:latest
docker push featurehub/edge:latest



