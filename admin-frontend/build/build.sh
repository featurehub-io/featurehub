#!/bin/sh
cd /opt/app/app
flutter pub get
echo building deploy_main
flutter build web --target=lib/deploy_main.dart
echo finished building
