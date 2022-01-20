#!/bin/sh
if [ $# -eq 0 ]
  then
  echo DEBUG=true npm run test
  DEBUG=true npm run test
else
  echo DEBUG=true npm run test -- --tags $1
  DEBUG=true npm run test -- --tags $1
fi

