#!/bin/sh
cd /opt/app/app
echo FLUTTER: Cleaning up after last build
rm -rf build
flutter clean
flutter pub get
echo FLUTTER: building deploy_main
flutter analyze
if test "$?" != "0"; then
  echo "failed"
  exit 1
fi
flutter build web --target=lib/deploy_main.dart
echo FLUTTER: finished building
exit 0
