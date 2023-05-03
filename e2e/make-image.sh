#!/bin/bash
if [[ ! -L "app_mr_layer" ]]; then
  ln -s ../admin-frontend/app_mr_layer app_mr_layer
fi
cd app_mr_layer && mvn generate-sources
cd ..
tar -ch . | docker build -t featurehub/e2e-dart:1.0 -
