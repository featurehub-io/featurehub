import 'package:flutter/material.dart';

final ThemeData myTheme = ThemeData(
    colorSchemeSeed: Colors.blueAccent.shade700,
    useMaterial3: true,
    brightness: Brightness.light,
        );
final ThemeData darkTheme = ThemeData(
  useMaterial3: true,
  brightness: Brightness.dark,
  colorSchemeSeed: Colors.blueAccent.shade200,
);
