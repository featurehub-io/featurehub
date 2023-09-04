#!/bin/sh
mkdir -p logs
rm -rf logs/*
if [ $# -eq 0 ]
  then
  echo DEBUG=true npm run e2e-test
  DEBUG=true npm run e2e-test
else
  echo DEBUG=true npm run e2e-test -- --tags $1
  DEBUG=true npm run e2e-test -- --tags $1
fi

