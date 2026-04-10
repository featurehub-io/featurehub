import 'package:flutter/material.dart';
import 'package:shared_preferences/shared_preferences.dart';

typedef LocaleWidgetBuilder = Widget Function(BuildContext context, Locale locale);

class DynamicLocale extends StatefulWidget {
  const DynamicLocale({
    super.key,
    required this.defaultLocale,
    required this.localeWidgetBuilder,
  });

  final Locale defaultLocale;
  final LocaleWidgetBuilder localeWidgetBuilder;

  static DynamicLocaleState of(BuildContext context) {
    return context.findAncestorStateOfType()!;
  }

  @override
  DynamicLocaleState createState() => DynamicLocaleState();
}

class DynamicLocaleState extends State<DynamicLocale> {
  static const String _prefsKey = 'locale';

  late Locale _locale;

  Locale get locale => _locale;

  @override
  void initState() {
    super.initState();
    _locale = widget.defaultLocale;
    SharedPreferences.getInstance().then((prefs) {
      final saved = prefs.getString(_prefsKey);
      if (saved != null && mounted) {
        setState(() => _locale = Locale(saved));
      }
    });
  }

  Future<void> setLocale(Locale locale) async {
    setState(() => _locale = locale);
    final prefs = await SharedPreferences.getInstance();
    await prefs.setString(_prefsKey, locale.languageCode);
  }

  @override
  Widget build(BuildContext context) {
    return widget.localeWidgetBuilder(context, _locale);
  }
}
