#!/bin/sh
BRANCH=$(git rev-parse --abbrev-ref HEAD)
docker build --build-arg branch=${BRANCH} -t featurehub/e2e-adk:1.0 .
