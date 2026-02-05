#!/bin/sh
VERSION=1.14
docker build -t featurehub/flutter_web:${VERSION} .
docker tag featurehub/flutter_web:${VERSION} us-central1-docker.pkg.dev/demohub-283022/demohub/build-images/flutter-build:${VERSION}

