#!/bin/sh
docker build -t featurehub/flutter_web:1.12 .
docker tag featurehub/flutter_web:1.12 us-central1-docker.pkg.dev/demohub-283022/demohub/build-images/flutter-build:1.12

