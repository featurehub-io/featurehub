#!/bin/sh
if [ "$1" = "" ]; then
  echo Must specify a build version as first parameter
  exit -1
fi
rm -rf target/build_web
mkdir -p target/build_web
cp build/build.sh target
chmod ugo+x target/build.sh
cd target
tar xvf *.tar
docker run --rm -e BUILD_VERSION="$1" -v $PWD:/opt/app --entrypoint /bin/bash featurehub/flutter_web:1.11 -c "cd /opt/app && ./build.sh"

#docker run -it -v $PWD/open_admin_app:/opt/app/app -v $PWD/app_mr_layer:/opt/app/app_mr_layer -v $PWD/build:/opt/build featurehub/flutter_web:1.1 /bin/sh /opt/build/build.sh

