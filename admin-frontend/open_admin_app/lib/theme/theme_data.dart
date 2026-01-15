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
      indicatorColor: const Color(
          0xffD1D7FF), // Darker shade of primary blue-purple (#536DFE)
    ),
    listTileTheme: ListTileThemeData(
      selectedTileColor:
          const Color(0xffD1D7FF), // Darker shade of action button color
      selectedColor: flexSchemeLight.primary, // Blue-purple text when selected
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
    ),
    // Input Fields - match background color to hide inner box in dropdowns
    inputDecorationTheme: InputDecorationTheme(
      filled: false, // No filled background to avoid inner rectangular box
      fillColor: flexSchemeLight
          .primaryContainer, // Match background to hide inner box
    ));

final ThemeData darkTheme = ThemeData(
  useMaterial3: true,
  brightness: Brightness.dark,
  colorScheme: flexSchemeDark,
  scaffoldBackgroundColor: const Color(0xff0A0A0A), // Pure black background

  // App Bar - very dark grey/off-black
  appBarTheme: const AppBarTheme(
    backgroundColor: Color(0xff1E1E1E), // Very dark grey
    foregroundColor: Color(0xffE0E0E0), // Light text
    elevation: 0,
    surfaceTintColor: Colors.transparent, // No tinting
  ),

  // Drawer/Menu - darker blue-grey so it blends with the dark shell
  drawerTheme: const DrawerThemeData(
    backgroundColor: Color(0xff1D222C), // Dark blue-grey for menu
    surfaceTintColor: Colors.transparent, // No tinting
  ),

  // Navigation Rail - matches drawer with darker blue-grey
  navigationRailTheme: const NavigationRailThemeData(
    backgroundColor: Color(0xff1D222C), // Dark blue-grey menu
    selectedIconTheme:
        IconThemeData(color: Color(0xffE0E0E0)), // Light icons when selected
    unselectedIconTheme:
        IconThemeData(color: Color(0xff808080)), // Medium grey when unselected
    selectedLabelTextStyle:
        TextStyle(color: Color(0xffE0E0E0)), // Light text when selected
    unselectedLabelTextStyle:
        TextStyle(color: Color(0xff808080)), // Medium grey when unselected
    indicatorColor:
        Color(0xff242B38), // Very dark blue-grey for selected item background
  ),

  // Cards - similar blue-grey to menu, slightly darker for depth
  cardTheme: CardThemeData(
    color: const Color(
        0xff181D26), // Darker blue-grey for cards than menu (#1D222C)
    elevation: 2,
    surfaceTintColor: Colors.transparent, // No tinting
    shadowColor: Colors.black.withOpacity(0.5), // Black shadows
  ),

  // List Tiles - for menu items with blue-grey selected state
  listTileTheme: const ListTileThemeData(
    selectedTileColor: Color(0xff38455B), // Blue-grey for selected menu items
    selectedColor: Color(0xffE0E0E0), // Light text when selected
    iconColor: Color(0xff808080), // Medium grey icons
    textColor: Color(0xffE0E0E0), // Light text
  ),

  // Input Fields - allow individual TextFields to specify their own borders
  inputDecorationTheme: InputDecorationTheme(
    filled: false, // Transparent background
    labelStyle: const TextStyle(color: Color(0xffB0B0B0)), // Medium grey labels
    hintStyle: const TextStyle(color: Color(0xff606060)), // Darker grey hints
  ),

  // Buttons - with color to stand out
  elevatedButtonTheme: ElevatedButtonThemeData(
    style: ElevatedButton.styleFrom(
      backgroundColor:
          const Color(0xff536DFE), // Blue accent color for primary buttons
      foregroundColor: const Color(0xffFFFFFF), // White text
      elevation: 2,
      surfaceTintColor: Colors.transparent,
      padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 12),
    ),
  ),

  filledButtonTheme: FilledButtonThemeData(
    style: FilledButton.styleFrom(
      backgroundColor: const Color(0xff536DFE), // Blue accent color
      foregroundColor: const Color(0xffFFFFFF), // White text
      surfaceTintColor: Colors.transparent,
      padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 12),
    ),
  ),

  outlinedButtonTheme: OutlinedButtonThemeData(
    style: OutlinedButton.styleFrom(
      foregroundColor:
          const Color(0xff536DFE), // Blue text for outlined buttons
      side: const BorderSide(color: Color(0xff536DFE)), // Blue border
      padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 12),
    ),
  ),

  textButtonTheme: TextButtonThemeData(
    style: TextButton.styleFrom(
      foregroundColor: const Color(0xff536DFE), // Blue text for text buttons
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
    ),
  ),

  // Data Tables
  dataTableTheme: const DataTableThemeData(
    headingTextStyle: TextStyle(
      fontSize: 14,
      fontWeight: FontWeight.w800,
      color: Color(0xffE0E0E0), // Light text
    ),
    dataTextStyle: TextStyle(color: Color(0xffE0E0E0)),
  ),

  // Snackbars
  snackBarTheme: SnackBarThemeData(
    backgroundColor: Colors.orange,
    contentTextStyle: const TextStyle(
        color: Colors.white), // White text for better contrast on orange
    actionTextColor: Colors.white, // White action text for better contrast
  ),

  // Dividers
  dividerTheme: const DividerThemeData(
    color: Color(0xff404040), // Medium grey dividers
    thickness: 1.0,
    space: 1.0,
  ),

  // Bottom Sheets
  bottomSheetTheme: const BottomSheetThemeData(
    backgroundColor: Color(0xff1E1E1E), // Very dark grey
    surfaceTintColor: Colors.transparent,
  ),

  // Dialogs - match the blue-grey theme
  dialogTheme: const DialogThemeData(
    backgroundColor: Color(0xff1D222C), // Dark blue-grey to match menu
    surfaceTintColor: Colors.transparent,
    titleTextStyle: TextStyle(
      color: Color(0xffE0E0E0),
      fontSize: 20,
      fontWeight: FontWeight.w600,
    ),
    contentTextStyle: TextStyle(color: Color(0xffE0E0E0)),
  ),

  // Text Theme - ensure all text is properly colored
  textTheme: const TextTheme(
    displayLarge: TextStyle(color: Color(0xffE0E0E0)),
    displayMedium: TextStyle(color: Color(0xffE0E0E0)),
    displaySmall: TextStyle(color: Color(0xffE0E0E0)),
    headlineLarge: TextStyle(color: Color(0xffE0E0E0)),
    headlineMedium: TextStyle(color: Color(0xffE0E0E0)),
    headlineSmall: TextStyle(color: Color(0xffE0E0E0)),
    titleLarge: TextStyle(color: Color(0xffE0E0E0)),
    titleMedium: TextStyle(color: Color(0xffE0E0E0)),
    titleSmall: TextStyle(color: Color(0xffE0E0E0)),
    bodyLarge: TextStyle(color: Color(0xffE0E0E0)),
    bodyMedium: TextStyle(color: Color(0xffE0E0E0)),
    bodySmall:
        TextStyle(color: Color(0xffB0B0B0)), // Slightly muted for small text
    labelLarge: TextStyle(color: Color(0xffE0E0E0)),
    labelMedium: TextStyle(color: Color(0xffB0B0B0)),
    labelSmall: TextStyle(color: Color(0xffE0E0E0)),
  ),

  // Icon Theme
  iconTheme: const IconThemeData(
    color: Color(0xffB0B0B0), // Medium grey icons
  ),

  // Primary Icon Theme
  primaryIconTheme: const IconThemeData(
    color: Color(0xffE0E0E0), // Light icons for primary actions
  ),
);

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
  tertiaryContainer:
      Color(0xffD1D7FF), // Darker shade of primary blue-purple (#536DFE)
  onTertiaryContainer: Color(0xff2c3e50), // Dark text on light blue-purple
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

// Professional black/off-black dark theme color scheme with blue-grey accents
const ColorScheme flexSchemeDark = ColorScheme(
  brightness: Brightness.dark,
  // Primary color for buttons and accents
  primary: Color(0xff536DFE), // Blue accent for buttons
  onPrimary: Color(0xffFFFFFF), // White text on blue buttons
  primaryContainer: Color(0xff2C2F3A), // Blue-grey for menu/drawer background
  onPrimaryContainer: Color(0xffE0E0E0), // Light text on menu
  secondary: Color(0xffB0B0B0), // Medium grey for secondary elements
  onSecondary: Color(0xff1A1A1A),
  secondaryContainer: Color(0xff2C2F3A), // Blue-grey for consistency
  onSecondaryContainer: Color(0xffE0E0E0),
  tertiary: Color(0xff909090), // Slightly darker grey for tertiary
  onTertiary: Color(0xff1A1A1A),
  tertiaryContainer: Color(0xff38455B), // Blue-grey for selected items
  onTertiaryContainer: Color(0xffE0E0E0),
  error: Color(0xffEF5350), // red for errors
  onError: Color(0xffFFFFFF),
  errorContainer: Color(0xff5C1A1A), // Dark red background
  onErrorContainer: Color(0xffFFB4AB),
  // Surface colors - black and off-black palette
  surface: Color(0xff1A1A1A), // Very dark grey/off-black for main background
  onSurface: Color(0xffE0E0E0), // Light text on dark surface
  surfaceVariant: Color(0xff252525), // Slightly lighter for cards
  onSurfaceVariant: Color(0xffB0B0B0), // Muted text
  outline: Color(0xff404040), // Medium grey for borders/outlines
  outlineVariant: Color(0xff2C2C2C), // Darker outline variant
  shadow: Color(0xff000000), // Pure black shadows
  scrim: Color(0xff000000), // Pure black scrim
  inverseSurface: Color(0xffE0E0E0),
  onInverseSurface: Color(0xff1A1A1A),
  inversePrimary: Color(0xff2C2C2C), // Neutral dark grey
  surfaceTint: Color(0x00000000), // Transparent - no tinting
);
