#!/bin/sh
cd /opt/app/app
flutter pub get
flutter build web --target=lib/deploy_main.dart
