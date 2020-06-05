#!/bin/sh
rm -rf m2-repo
mkdir -p m2-repo
cp -R ~/.m2/repository m2-repo
docker build -t gcr.io/featurehub/basemvn:1.1 . && rm -rf m2-repo
