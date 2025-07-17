#!/bin/sh
#docker buildx create --use --config buildtoml --name
docker buildx create --use  --name party-builder
docker buildx build $DOCKER_OPTS --platform linux/amd64,linux/arm64 . -t featurehub/base_jdk:1.13 --push
docker buildx rm party-builder
