import 'package:flutter/material.dart';

final ThemeData myTheme = ThemeData(
    colorScheme: flexSchemeLight,
    useMaterial3: true,
    brightness: Brightness.light,
    appBarTheme: AppBarTheme(
      backgroundColor: flexSchemeLight.primaryContainer,
      foregroundColor: flexSchemeLight.onSurface,
      elevation: 0,
    ),
    drawerTheme: DrawerThemeData(
      backgroundColor: flexSchemeLight.primaryContainer,
      surfaceTintColor: flexSchemeLight.primaryContainer,
    ),
    navigationRailTheme: NavigationRailThemeData(
      backgroundColor: flexSchemeLight.primaryContainer,
      selectedIconTheme: IconThemeData(color: flexSchemeLight.primary),
      unselectedIconTheme: IconThemeData(
          color: flexSchemeLight.onPrimaryContainer.withAlpha(179)),
      selectedLabelTextStyle: TextStyle(color: flexSchemeLight.primary),
      unselectedLabelTextStyle:
          TextStyle(color: flexSchemeLight.onPrimaryContainer.withAlpha(179)),
    ),
    cardTheme: CardThemeData(
      color: flexSchemeLight.primaryContainer,
      elevation: 3,
      surfaceTintColor: Colors.transparent,
      shadowColor: flexSchemeLight.shadow.withAlpha(51),
    ),
    dataTableTheme: const DataTableThemeData(
        headingTextStyle: TextStyle(
            fontSize: 14, fontWeight: FontWeight.w800, color: Colors.black87)),
    snackBarTheme: const SnackBarThemeData(backgroundColor: Colors.orange),
    dividerTheme: DividerThemeData(
      color: flexSchemeLight.outline,
      thickness: 1.0,
    ));

final ThemeData darkTheme = ThemeData(
    useMaterial3: true,
    brightness: Brightness.dark,
    colorScheme: flexSchemeDark,
    appBarTheme: AppBarTheme(
      backgroundColor: flexSchemeDark.surface,
      foregroundColor: flexSchemeDark.onSurface,
      elevation: 0,
    ),
    drawerTheme: DrawerThemeData(
      backgroundColor: flexSchemeDark.primaryContainer,
      surfaceTintColor: flexSchemeDark.primaryContainer,
    ),
    navigationRailTheme: NavigationRailThemeData(
      backgroundColor: flexSchemeDark.primaryContainer,
      selectedIconTheme:
          IconThemeData(color: flexSchemeDark.onPrimaryContainer),
      unselectedIconTheme: IconThemeData(
          color: flexSchemeDark.onPrimaryContainer.withAlpha(179)),
      selectedLabelTextStyle:
          TextStyle(color: flexSchemeDark.onPrimaryContainer),
      unselectedLabelTextStyle:
          TextStyle(color: flexSchemeDark.onPrimaryContainer.withAlpha(179)),
    ),
    cardTheme: CardThemeData(
      color: flexSchemeDark.primaryContainer,
      elevation: 2,
      surfaceTintColor: Colors.transparent,
      shadowColor: flexSchemeDark.shadow.withAlpha(77),
    ),
    dataTableTheme: const DataTableThemeData(
        headingTextStyle: TextStyle(
            fontSize: 14, fontWeight: FontWeight.w800, color: Colors.white)),
    snackBarTheme: SnackBarThemeData(
        backgroundColor: Colors.orange.shade800.withAlpha(179)),
    dividerTheme: DividerThemeData(
      color: flexSchemeDark.outline,
      thickness: 1.0,
    ));

// Light and dark ColorSchemes made by FlexColorScheme v7.0.1.
// These ColorScheme objects require Flutter 3.7 or later.
const ColorScheme flexSchemeLight = ColorScheme(
  brightness: Brightness.light,
  primary: Color(0xff536DFE),
  onPrimary: Color(0xffffffff),
  primaryContainer: Color(0xffe9ecef),
  onPrimaryContainer: Color(0xff2c3e50),
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
  surface: Color(0xfff8f9fa),
  onSurface: Color(0xff212529),
  outline: Color(0xffadb5bd),
  outlineVariant: Color(0xffdee2e6),
  shadow: Color(0xff000000),
  scrim: Color(0xff000000),
  inverseSurface: Color(0xff111112),
  onInverseSurface: Color(0xfffafafa),
  inversePrimary: Color(0xffaedfff),
  surfaceTint: Color(0xff1565c0),
);

const ColorScheme flexSchemeDark = ColorScheme(
  brightness: Brightness.dark,
  primary: Color(0xff64a1f1),
  onPrimary: Color(0xff0c1014),
  primaryContainer: Color(0xff1e2a3a),
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
  surface: Color(0xff1a2330),
  onSurface: Color(0xfff5f5f5),
  outline: Color(0xff797979),
  outlineVariant: Color(0xff2d2d2d),
  shadow: Color(0xff000000),
  scrim: Color(0xff000000),
  inverseSurface: Color(0xfffcfdfe),
  onInverseSurface: Color(0xff090909),
  inversePrimary: Color(0xff3d5168),
  surfaceTint: Color(0xff72a2d9),
);
