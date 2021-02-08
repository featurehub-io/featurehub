import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';

final fontFamily = GoogleFonts.roboto().fontFamily;

final ThemeData myTheme = ThemeData(
  // primarySwatch: MaterialColor(4278391389,{50: Instance of 'minified:i', 100: Instance of 'minified:i', 200: Instance of 'minified:i', 300: Instance of 'minified:i', 400: Instance of 'minified:i', 500: Instance of 'minified:i', 600: Instance of 'minified:i', 700: Instance of 'minified:i', 800: Instance of 'minified:i', 900: Instance of 'minified:i'}),
  brightness: Brightness.light,
  primaryColor: Color(0xff03125d),
  primaryColorBrightness: Brightness.dark,
  primaryColorLight: Color(0xffced6fd),
  primaryColorDark: Color(0xff051d94),
  accentColor: Colors.orange,
  accentColorBrightness: Brightness.dark,
  canvasColor: Color(0xffffffff),
  scaffoldBackgroundColor: Color(0xfffafafa),
  bottomAppBarColor: Color(0xffffffff),
  cardColor: Color(0xffffffff),
  dividerColor: Color(0x1f000000),
  highlightColor: Color(0x66bcbcbc),
  splashColor: Color(0x66c8c8c8),
  selectedRowColor: Color(0xfff5f5f5),
  unselectedWidgetColor: Color(0x8a000000),
  disabledColor: Color(0x61000000),
  buttonColor: Color(0xff473e8f),
  toggleableActiveColor: Color(0xff678de5),
  secondaryHeaderColor: Color(0xfff6f6f6),

  backgroundColor: Color(0x226DD3F4),
  dialogBackgroundColor: Color(0xffffffff),
  indicatorColor: Color(0xff051d94),
  hintColor: Color(0x8a000000),
  errorColor: Color(0xffd32f2f),
  textSelectionTheme: TextSelectionThemeData(
    selectionColor: Color(0xff9cacfc),
    cursorColor: Color(0xff4285f4),
    selectionHandleColor: Color(0xff6b83fa),
  ),
  textButtonTheme: TextButtonThemeData(
    style: TextButton.styleFrom(primary: Color(0xff473e8f)),
  ),
  buttonTheme: ButtonThemeData(
    textTheme: ButtonTextTheme.normal,
    minWidth: 88,
    height: 36,
    padding: EdgeInsets.only(top: 16, bottom: 16, left: 16, right: 16),
    shape: RoundedRectangleBorder(
      side: BorderSide(
        color: Color(0xff000000),
        width: 0,
        style: BorderStyle.none,
      ),
      borderRadius: BorderRadius.all(Radius.circular(4.0)),
    ),
    alignedDropdown: false,
    buttonColor: Color(0xff5671e9),
    disabledColor: Color(0x61000000),
    highlightColor: Color(0x29000000),
    splashColor: Color(0x1f000000),
    colorScheme: ColorScheme(
      primary: Color(0xff03125d),
      primaryVariant: Color(0xff051d94),
      secondary: Color(0xff0830f7),
      secondaryVariant: Color(0xff051d94),
      surface: Color(0xffffffff),
      background: Color(0xff9cacfc),
      error: Color(0xffd32f2f),
      onPrimary: Color(0xffffffff),
      onSecondary: Color(0xffffffff),
      onSurface: Color(0xff000000),
      onBackground: Color(0xffffffff),
      onError: Color(0xffffffff),
      brightness: Brightness.light,
    ),
  ),
  fontFamily: GoogleFonts.roboto().fontFamily,

  inputDecorationTheme: InputDecorationTheme(
    labelStyle: TextStyle(
      color: Color(0xff473e8f),
      fontSize: 16,
      fontWeight: FontWeight.w400,
      fontStyle: FontStyle.normal,
    ),
    helperStyle: TextStyle(
      color: Color(0xff4a4a4a),
      fontSize: 12,
      fontWeight: FontWeight.w400,
      fontStyle: FontStyle.normal,
    ),
    hintStyle: TextStyle(
      color: Color(0xff4a4a4a),
      fontSize: 12,
      fontWeight: FontWeight.w400,
      fontStyle: FontStyle.normal,
    ),
    errorStyle: TextStyle(
      color: Color(0xffd32f2f),
      fontSize: 12,
      fontWeight: FontWeight.w400,
      fontStyle: FontStyle.normal,
    ),
    errorMaxLines: null,
    isDense: false,
    contentPadding: EdgeInsets.only(top: 12, bottom: 12, left: 0, right: 0),
    isCollapsed: false,
    prefixStyle: TextStyle(
      color: Color(0xdd000000),
      fontSize: 16,
      fontWeight: FontWeight.w400,
      fontStyle: FontStyle.normal,
    ),
    suffixStyle: TextStyle(
      color: Color(0xdd000000),
      fontSize: 16,
      fontWeight: FontWeight.w400,
      fontStyle: FontStyle.normal,
    ),
    counterStyle: TextStyle(
      color: Color(0xdd000000),
      fontSize: 16,
      fontWeight: FontWeight.w400,
      fontStyle: FontStyle.normal,
    ),
    filled: false,
    fillColor: Color(0xffF5F5F5),
    errorBorder: UnderlineInputBorder(
      borderSide: BorderSide(
        color: Color(0xffd32f2f),
        width: 1,
        style: BorderStyle.solid,
      ),
      borderRadius: BorderRadius.all(Radius.circular(4.0)),
    ),
    focusedBorder: UnderlineInputBorder(
      borderSide: BorderSide(
        color: Color(0xff473e8f),
        width: 1,
        style: BorderStyle.solid,
      ),
    ),
    focusedErrorBorder: UnderlineInputBorder(
      borderSide: BorderSide(
        color: Color(0xffd32f2f),
        width: 2,
        style: BorderStyle.solid,
      ),
    ),
    disabledBorder: UnderlineInputBorder(
      borderSide: BorderSide(
        color: Color(0xff000000),
        width: 1,
        style: BorderStyle.solid,
      ),
      borderRadius: BorderRadius.all(Radius.circular(4.0)),
    ),
    enabledBorder: UnderlineInputBorder(
      borderSide: BorderSide(
        color: Color(0xff000000),
        width: 1,
        style: BorderStyle.solid,
      ),
      borderRadius: BorderRadius.only(
          topLeft: Radius.circular(4.0), topRight: Radius.circular(4.0)),
    ),
    border: UnderlineInputBorder(
      borderSide: BorderSide(
        color: Color(0xff000000),
        width: 1,
        style: BorderStyle.solid,
      ),
      borderRadius: BorderRadius.only(
          topLeft: Radius.circular(4.0), topRight: Radius.circular(4.0)),
    ),
  ),
  iconTheme: IconThemeData(
    color: Color(0xff473e8f),
    opacity: 1,
    size: 24,
  ),
  primaryIconTheme: IconThemeData(
    color: Color(0xffffffff),
    opacity: 1,
    size: 24,
  ),
  accentIconTheme: IconThemeData(
    color: Color(0xffffffff),
    opacity: 1,
    size: 24,
  ),
  sliderTheme: SliderThemeData(
    activeTrackColor: Color(0xff03125d),
    inactiveTrackColor: Color(0x3d03125d),
    disabledActiveTrackColor: Color(0x52020a31),
    disabledInactiveTrackColor: Color(0x1f020a31),
    activeTickMarkColor: Color(0x8ae6eafe),
    inactiveTickMarkColor: Color(0x8a03125d),
    disabledActiveTickMarkColor: Color(0x1fe6eafe),
    disabledInactiveTickMarkColor: Color(0x1f020a31),
    thumbColor: Color(0xff03125d),
    disabledThumbColor: Color(0x52020a31),
    thumbShape: RoundSliderThumbShape(),
    overlayColor: Color(0x1f03125d),
    valueIndicatorColor: Color(0xff03125d),
    valueIndicatorShape: PaddleSliderValueIndicatorShape(),
    showValueIndicator: ShowValueIndicator.onlyForDiscrete,
    valueIndicatorTextStyle: TextStyle(
      color: Color(0xffffffff),
      fontSize: 14,
      fontWeight: FontWeight.w400,
      fontStyle: FontStyle.normal,
    ),
  ),
  tabBarTheme: TabBarTheme(
    indicatorSize: TabBarIndicatorSize.tab,
    labelColor: Color(0xffffffff),
    unselectedLabelColor: Color(0xb2ffffff),
  ),
  chipTheme: ChipThemeData(
    backgroundColor: Color(0x3d03125d),
    brightness: Brightness.light,
    deleteIconColor: Color(0xde03125d),
    disabledColor: Color(0x0c000000),
    labelPadding: EdgeInsets.only(top: 0, bottom: 0, left: 8, right: 8),
    labelStyle: TextStyle(
      color: Color(0xde03125d),
      fontSize: 14,
      fontWeight: FontWeight.w400,
      fontStyle: FontStyle.normal,
    ),
    padding: EdgeInsets.only(top: 4, bottom: 4, left: 4, right: 4),
    secondaryLabelStyle: TextStyle(
      color: Color(0x3d03125d),
      fontSize: 14,
      fontWeight: FontWeight.w400,
      fontStyle: FontStyle.normal,
    ),
    secondarySelectedColor: Color(0x3d03125d),
    selectedColor: Color(0x3d03125d),
    shape: StadiumBorder(
        side: BorderSide(
      color: Color(0xff000000),
      width: 0,
      style: BorderStyle.none,
    )),
  ),
  dialogTheme: DialogTheme(
      shape: RoundedRectangleBorder(
    side: BorderSide(
      color: Color(0xff000000),
      width: 0,
      style: BorderStyle.none,
    ),
    borderRadius: BorderRadius.all(Radius.circular(4.0)),
  )),
);
final ThemeData darkTheme = ThemeData(
  primarySwatch: Colors.blue,
  brightness: Brightness.dark,
  fontFamily: fontFamily,
);
