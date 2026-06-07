#!/bin/sh
mkdir -p logs
rm -rf logs/*
if [ $# -eq 0 ]
  then
  if [ "$SAAS_ORGANISATION_ID" != "" ]; then
    DEBUG=true pnpm run test --tags "not @notsaas"
  else
    DEBUG=true pnpm run test --tags "not @saasflags"
  fi
else
  DEBUG=true pnpm run test --tags $1
fi

