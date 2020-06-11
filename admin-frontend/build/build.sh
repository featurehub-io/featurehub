#!/bin/sh
cd /opt/app/app
flutter pub get
echo building deploy_main
flutter analyze
if test "$?" != "0"; then
  echo "failed"
  exit 1
fi
flutter build web --target=lib/deploy_main.dart
echo finished building
exit 0
