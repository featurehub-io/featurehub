#!/bin/sh
mkdir -p logs
rm -rf logs/*
if [ $# -eq 0 ]
  then
  if [ "$INGRESS_IP" != "" ]; then
    # we don't want these tests in kind/local k8s as they don't work
    DEBUG=true pnpm run test --tags "not @notkind"
  elif [ "$SAAS_ORGANISATION_ID" != "" ]; then
    DEBUG=true pnpm run test --tags "not @notsaas"
  else
    DEBUG=true pnpm run test --tags "not @saasflags"
  fi
else
  DEBUG=true pnpm run test --tags $1
fi

