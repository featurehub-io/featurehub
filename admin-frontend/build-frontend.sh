#!/bin/sh
if [ "$1" = "" ]; then
  echo Must specify a build version as first parameter
  exit -1
fi
rm -rf target/build_web
mkdir -p target/build_web
cp build/build.sh target
cd target
#docker run -it -v $PWD/app_singleapp:/opt/app/app -v $PWD/app_mr_layer:/opt/app/app_mr_layer -v $PWD/build:/opt/build featurehub/flutter_web:1.1 /bin/sh /opt/build/build.sh
docker run --rm -e BUILD_VERSION="$1" -v $PWD:/opt/app featurehub/flutter_web:1.3 /bin/sh /opt/app/build.sh

