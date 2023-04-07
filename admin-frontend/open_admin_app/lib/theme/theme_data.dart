import 'package:flutter/material.dart';

final ThemeData myTheme = ThemeData(
  colorScheme: flexSchemeLight,
  useMaterial3: true,
  brightness: Brightness.light,
  dataTableTheme: const DataTableThemeData(
      headingTextStyle: TextStyle(
          fontSize: 14, fontWeight: FontWeight.w800, color: Colors.black87)),
);

final ThemeData darkTheme = ThemeData(
  useMaterial3: true,
  brightness: Brightness.dark,
  colorScheme: flexSchemeDark,
  dataTableTheme: const DataTableThemeData(
      headingTextStyle: TextStyle(
          fontSize: 14, fontWeight: FontWeight.w800, color: Colors.white)),
);

// Light and dark ColorSchemes made by FlexColorScheme v7.0.1.
// These ColorScheme objects require Flutter 3.7 or later.
const ColorScheme flexSchemeLight = ColorScheme(
  brightness: Brightness.light,
  primary: Color(0xff1565c0),
  onPrimary: Color(0xffffffff),
  primaryContainer: Color(0xffb8d5f7),
  onPrimaryContainer: Color(0xff101214),
  secondary: Color(0xff039be5),
  onSecondary: Color(0xffffffff),
  secondaryContainer: Color(0xff92deff),
  onSecondaryContainer: Color(0xff0d1214),
  tertiary: Color(0xff0277bd),
  onTertiary: Color(0xffffffff),
  tertiaryContainer: Color(0xff97d1f4),
  onTertiaryContainer: Color(0xff0d1114),
  error: Color(0xffba1a1a),
  onError: Color(0xffffffff),
  errorContainer: Color(0xffffdad6),
  onErrorContainer: Color(0xff410002),
  background: Color(0xfffbfcfe),
  onBackground: Color(0xff090909),
  surface: Color(0xfffdfdfe),
  onSurface: Color(0xff040404),
  surfaceVariant: Color(0xffeaebed),
  onSurfaceVariant: Color(0xff090909),
  outline: Color(0xff7c7c7c),
  outlineVariant: Color(0xffc8c8c8),
  shadow: Color(0xff000000),
  scrim: Color(0xff000000),
  inverseSurface: Color(0xff111112),
  onInverseSurface: Color(0xfffafafa),
  inversePrimary: Color(0xffaedfff),
  surfaceTint: Color(0xff1565c0),
);

const ColorScheme flexSchemeDark = ColorScheme(
  brightness: Brightness.dark,
  primary: Color(0xff72a2d9),
  onPrimary: Color(0xff0c1014),
  primaryContainer: Color(0xff2a435f),
  onPrimaryContainer: Color(0xffe6eaee),
  secondary: Color(0xff67c3ef),
  onSecondary: Color(0xff0c1314),
  secondaryContainer: Color(0xff0a4561),
  onSecondaryContainer: Color(0xffe1eaef),
  tertiary: Color(0xff67add7),
  onTertiary: Color(0xff0c1114),
  tertiaryContainer: Color(0xff14374c),
  onTertiaryContainer: Color(0xffe2e8eb),
  error: Color(0xffffb4ab),
  onError: Color(0xff690005),
  errorContainer: Color(0xff93000a),
  onErrorContainer: Color(0xffffb4ab),
  background: Color(0xff141618),
  onBackground: Color(0xffececec),
  surface: Color(0xff121314),
  onSurface: Color(0xfff5f5f5),
  surfaceVariant: Color(0xff343638),
  onSurfaceVariant: Color(0xffefefef),
  outline: Color(0xff797979),
  outlineVariant: Color(0xff2d2d2d),
  shadow: Color(0xff000000),
  scrim: Color(0xff000000),
  inverseSurface: Color(0xfffcfdfe),
  onInverseSurface: Color(0xff090909),
  inversePrimary: Color(0xff3d5168),
  surfaceTint: Color(0xff72a2d9),
);
