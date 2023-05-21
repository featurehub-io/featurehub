#!/bin/sh
docker build -t featurehub/flutter_web:1.11 .
docker tag featurehub/flutter_web:1.11 us-central1-docker.pkg.dev/demohub-283022/demohub/build-images/flutter-build:1.11

