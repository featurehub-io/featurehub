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
  primary: Color(0xff0000ba),
  onPrimary: Color(0xffffffff),
  primaryContainer: Color(0xffe0e0ff),
  onPrimaryContainer: Color(0xff00006e),
  secondary: Color(0xff5a5b81),
  onSecondary: Color(0xffffffff),
  secondaryContainer: Color(0xfff2efff),
  onSecondaryContainer: Color(0xff17183a),
  tertiary: Color(0xff3f4178),
  onTertiary: Color(0xffffffff),
  tertiaryContainer: Color(0xffbfc1ff),
  onTertiaryContainer: Color(0xff12144b),
  error: Color(0xffba1a1a),
  onError: Color(0xffffffff),
  errorContainer: Color(0xffffdad6),
  onErrorContainer: Color(0xff410002),
  background: Color(0xfffefafe),
  onBackground: Color(0xff1b1b1f),
  surface: Color(0xfffefafe),
  onSurface: Color(0xff1b1b1f),
  surfaceVariant: Color(0xffe3e0eb),
  onSurfaceVariant: Color(0xff46464f),
  outline: Color(0xff777680),
  outlineVariant: Color(0xffc7c5d0),
  shadow: Color(0xff000000),
  scrim: Color(0xff000000),
  inverseSurface: Color(0xff2f2f34),
  onInverseSurface: Color(0xfff3eff4),
  inversePrimary: Color(0xffbfc2ff),
  surfaceTint: Color(0xff4049e0),
);

const ColorScheme flexSchemeDark = ColorScheme(
  brightness: Brightness.dark,
  primary: Color(0xffbfc2ff),
  onPrimary: Color(0xff0000ac),
  primaryContainer: Color(0xff232ac9),
  onPrimaryContainer: Color(0xffe0e0ff),
  secondary: Color(0xffc3c3ee),
  onSecondary: Color(0xff2c2d50),
  secondaryContainer: Color(0xff434368),
  onSecondaryContainer: Color(0xffe1e0ff),
  tertiary: Color(0xffe1e0ff),
  onTertiary: Color(0xff282a60),
  tertiaryContainer: Color(0xff565992),
  onTertiaryContainer: Color(0xfff1efff),
  error: Color(0xffffb4ab),
  onError: Color(0xff690005),
  errorContainer: Color(0xff93000a),
  onErrorContainer: Color(0xffffb4ab),
  background: Color(0xff1e1e24),
  onBackground: Color(0xffe5e1e6),
  surface: Color(0xff1d1d22),
  onSurface: Color(0xffe5e1e6),
  surfaceVariant: Color(0xff474751),
  onSurfaceVariant: Color(0xffc7c5d0),
  outline: Color(0xff918f9a),
  outlineVariant: Color(0xff46464f),
  shadow: Color(0xff000000),
  scrim: Color(0xff000000),
  inverseSurface: Color(0xffe4e0e6),
  onInverseSurface: Color(0xff303034),
  inversePrimary: Color(0xff4049e0),
  surfaceTint: Color(0xffbfc2ff),
);
