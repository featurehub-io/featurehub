#!/bin/sh
#docker run -it -v $PWD/app_singleapp:/opt/app/app -v $PWD/app_mr_layer:/opt/app/app_mr_layer -v $PWD/build:/opt/build featurehub/flutter_web:1.1 /bin/sh /opt/build/build.sh
docker run -v $PWD/app_singleapp:/opt/app/app -v $PWD/app_mr_layer:/opt/app/app_mr_layer -v $PWD/build:/opt/build featurehub/flutter_web:1.2 /bin/sh /opt/build/build.sh

