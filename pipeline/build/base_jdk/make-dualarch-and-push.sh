#!/bin/sh
#docker buildx create --use --config buildtoml --name
docker buildx create --use  --name party-builder
docker buildx build $DOCKER_OPTS --platform linux/amd64,linux/arm64 . -t featurehub/base_jdk:2.1 --push
docker buildx rm party-builder
