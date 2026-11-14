#!/bin/sh
#export HTTP_LOGGING=debug
$FLUTTER/bin/cache/dart-sdk/bin/dart --enable-asserts --enable-vm-service:54240 $PWD/test/test_party.dart "$@"
