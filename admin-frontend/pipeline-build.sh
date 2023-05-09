#!/bin/bash
if [ ! -f build-frontend ]; then
  echo "No front-end to build, existing"
  exit 0
fi
if which gsed >/dev/null; then
  alias sed=gsed
fi
echo flutter home is $FLUTTER
#echo "1.21.0-8.0.pre.110" > $FLUTTER_ROOT/version
#ls -la $FLUTTER
VERSION=`cat $FLUTTER/version`
DVERSION=`cat $FLUTTER/bin/cache/dart-sdk/version`
echo Flutter version is $VERSION dart version is $DVERSION
cd admin-frontend/app_mr_layer
flutter channel
flutter pub get
cd ../open_admin_app
flutter config --enable-web
flutter pub get
echo FLUTTER: building deploy_main
echo flutter analyze
flutter analyze
if test "$?" != "0"; then
  echo "failed"
  exit 1
fi
echo flutter build web --target=lib/deploy_main.dart
flutter build web --target=lib/deploy_main.dart
if test "$?" != "0"; then
  echo "failed"
  exit 1
fi
cd build/web
MAIN_SHA=`sha256sum main.dart.js | awk '{print $1}'`
MAIN="main.$MAIN_SHA.js"
sed -i s/main.dart.js/$MAIN/ index.html
mv main.dart.js $MAIN
mv main.dart.js.map $MAIN.map
echo FLUTTER: finished building, cleaning
exit 0

