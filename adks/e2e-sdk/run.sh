#!/bin/sh
mkdir -p logs
rm -rf logs/*
if [ $# -eq 0 ]
  then
  echo DEBUG=true npm run test
  DEBUG=true pnpm run test
else
  echo DEBUG=true npm run test --tags $1
  DEBUG=true pnpm run test --tags $1
fi

