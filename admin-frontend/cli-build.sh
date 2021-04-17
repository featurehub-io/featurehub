#!/bin/sh
build=${BUILD_NO:-0.0.1}
docker build -t featurehub/cli:${build} --no-cache -f cli/Dockerfile .
