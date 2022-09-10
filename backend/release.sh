#!/bin/sh
# make sure you have built all artifacts first
if [ $# -eq 0 ]
  then
    VERSION=`cat ../current-rc.txt`
else
  VERSION=$1
fi
if [[ $VERSION == *"RC"* ]]; then
  echo "Is RC, not also tagging latest"
else
  echo "Is not RC, tagging latest"
  BUILD_PARAMS="$BUILD_PARAMS -Djib.to.tags=latest"
fi
DOCKER_PREFIX="${OVERRIDE_DOCKER_PREFIX:-featurehub}"
echo mvn -f pom-packages.xml -DskipTests $BUILD_PARAMS -Dapp.baseimage.prefix=featurehub/ -Ddocker.project.prefix=$DOCKER_PREFIX -Dcloud-build=true -Dbuild.version=$VERSION clean install
while true; do
    read -p "Are you sure you wish to release (y/n): " yn
    case $yn in
        [Yy]* ) mvn -f pom-packages.xml -DskipTests $BUILD_PARAMS  -Dapp.baseimage.prefix=featurehub/  -Ddocker.project.prefix=$DOCKER_PREFIX -Dcloud-build=true -Dbuild.version=$VERSION clean install; break;;
        [Nn]* ) exit;;
        * ) echo "Please answer yes or no.";;
    esac
done


