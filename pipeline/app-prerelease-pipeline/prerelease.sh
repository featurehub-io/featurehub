#!/bin/sh
# make sure you have built all artifacts first
if [ $# -eq 0 ]
  then
    VERSION=`cat ../current-rc.txt`
else
  VERSION=$1
fi
if [[ $VERSION != *"RC"* ]]; then
  echo "cannot do a prerelease, tag is not -RC"
  exit 2
fi
DOCKER_PREFIX="${OVERRIDE_DOCKER_PREFIX:-featurehub}"
echo mvn -f pom-packages.xml -DskipTests $BUILD_PARAMS -Ddocker.project.prefix=$DOCKER_PREFIX -Dcloud-build=true -Dbuild.version=$VERSION clean install
mvn -f pom-packages.xml -DskipTests $BUILD_PARAMS -Ddocker.project.prefix=$DOCKER_PREFIX -Dcloud-build=true -Dbuild.version=$VERSION clean install


