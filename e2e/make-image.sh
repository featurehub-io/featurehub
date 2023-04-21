#!/bin/bash
tar -ch . | docker build -t featurehub/e2e-dart:1.0 -
