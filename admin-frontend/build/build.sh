#!/bin/bash
echo started
ls -l
set -exo pipefail
echo flutter home is $FLUTTER
#echo "1.21.0-8.0.pre.110" > $FLUTTER_ROOT/version
#ls -la $FLUTTER
HERE=${PWD}
cd ${FLUTTER}
VERSION=`git tag`
cd $HERE
DVERSION=`cat $FLUTTER/bin/cache/dart-sdk/version`
echo Flutter version is $VERSION dart version is $DVERSION
echo FLUTTER: Cleaning up after last build

cd app_mr_layer && flutter pub get
echo "Flutter App will be version: ${BUILD_VERSION}"
cd ../open_admin_app && echo "final appVersion = '${BUILD_VERSION}';" > lib/version.dart && flutter clean && flutter pub get

sed_in_place() {
  if [[ "$OSTYPE" == 'darwin'* ]]; then
    gsed -i "$@"
  else
    sed -i "$@"
  fi
}

# index.html is never cached, everything else is cached for days: so each file index.html leads to is given a name from
# its content. index.html loads flutter_bootstrap.js, which loads main.dart.js, or for a WebAssembly build main.dart.mjs
# and main.dart.wasm where the browser supports it.
rename_main_dart() {
  cd build/web
  for FILE in main.dart.js main.dart.mjs main.dart.wasm; do
    if test -f "$FILE"; then
      SHA=`sha256sum $FILE | awk '{print $1}'`
      RENAMED="main-$SHA.${FILE#main.dart.}"
      echo Renaming $FILE to $RENAMED
      sed_in_place "s/\"$FILE\"/\"$RENAMED\"/g" flutter_bootstrap.js
      mv $FILE $RENAMED
      if test -f "$FILE.map"; then
        mv $FILE.map $RENAMED.map
      fi
    fi
  done
  BOOTSTRAP_SHA=`sha256sum flutter_bootstrap.js | awk '{print $1}'`
  BOOTSTRAP="flutter_bootstrap-$BOOTSTRAP_SHA.js"
  sed_in_place s/flutter_bootstrap.js/$BOOTSTRAP/ index.html
  mv flutter_bootstrap.js $BOOTSTRAP
  cd ../..
}

echo FLUTTER: building deploy_main
#flutter analyze
#if test "$?" != "0"; then
#  echo "failed"
#  exit 1
#fi
echo "building normal version"
flutter build web --target=lib/deploy_main.dart --release --no-tree-shake-icons

rename_main_dart
mv build build_original
mkdir -p build/web/assets

echo "building canvaskit embedded version"
# Flutter already downloads Canvaskit, this just lets us use what it has already downloaded (the bootstrap loader
# takes it from --no-web-resources-cdn, the engine from FLUTTER_WEB_CANVASKIT_URL)
flutter build web --no-web-resources-cdn --dart-define=FLUTTER_WEB_CANVASKIT_URL=canvaskit/ --target=lib/deploy_main.dart  --release --no-tree-shake-icons

rename_main_dart

mv build/web build_original/web/intranet
rm -rf build

#echo "building wasm+canvaskit version"
#flutter build web --target=lib/deploy_main.dart --wasm  --release --no-tree-shake-icons

#mv build/web build_original/web/html
#rm -rf build

mv build_original build

echo FLUTTER: finished building, cleaning
exit 0
