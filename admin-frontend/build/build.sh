#!/bin/bash
cd /opt/app
#tar xf *.tar
ls -l
echo flutter home is $FLUTTER
#echo "1.21.0-8.0.pre.110" > $FLUTTER_ROOT/version
#ls -la $FLUTTER
VERSION=`cat $FLUTTER/version`
DVERSION=`cat $FLUTTER/bin/cache/dart-sdk/version`
echo Flutter version is $VERSION dart version is $DVERSION
echo FLUTTER: Cleaning up after last build

cd app_mr_layer && flutter pub get
echo "Flutter App will be version: ${BUILD_VERSION}"
cd ../open_admin_app && echo "final appVersion = '${BUILD_VERSION}';" > lib/version.dart && flutter clean && flutter pub get

rename_main_dart() {
  cd build/web
  MAIN_DATE=`date +"%s"`
  MAIN="main.dart-$MAIN_DATE.js"
  if [[ "$OSTYPE" == 'darwin'* ]]; then
    sed s/main.dart.js/$MAIN/ index.html > index2.html
    mv index.html index-old.html
    mv index2.html index.html
  else
    sed -i s/main.dart.js/$MAIN/ index.html
  fi
  mv main.dart.js $MAIN
  if test -f "main.dart.js.map"; then
    mv main.dart.js.map $MAIN.map
  fi
  cd ../..
}

echo FLUTTER: building deploy_main
#flutter analyze
#if test "$?" != "0"; then
#  echo "failed"
#  exit 1
#fi
flutter build web --target=lib/deploy_main.dart

rename_main_dart
mv build build_original
mkdir -p build/web/assets

# Flutter already downloads Canvaskit, this just lets us use what it has already downloaded
flutter build web --dart-define=FLUTTER_WEB_CANVASKIT_URL=/canvaskit/ --target=lib/deploy_main.dart

rename_main_dart

mv build/web build_original/web/intranet
rm -rf build
mv build_original build
echo FLUTTER: finished building, cleaning
exit 0
