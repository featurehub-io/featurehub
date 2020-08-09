#!/bin/sh
cd /opt/app/app
echo flutter home is $FLUTTER
#echo "1.21.0-8.0.pre.110" > $FLUTTER_ROOT/version
#ls -la $FLUTTER
VERSION=`cat $FLUTTER/version`
DVERSION=`cat $FLUTTER/bin/cache/dart-sdk/version`
echo Flutter version is $VERSION dart version is $DVERSION
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
cd build/web
MAIN_DATE=`date +"%s"`
MAIN="main.dart-$MAIN_DATE.js"
sed -i s/main.dart.js/$MAIN/ index.html
mv main.dart.js $MAIN
mv main.dart.js.map $MAIN.map
echo FLUTTER: finished building, cleaning
exit 0
