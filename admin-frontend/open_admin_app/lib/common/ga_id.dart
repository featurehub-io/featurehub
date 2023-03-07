import 'package:usage/usage_html.dart';

const String UA = ''; // put UA google analytics id here

Analytics? getGA() {
  if (UA.isNotEmpty) {
    Analytics ga = AnalyticsHtml(UA, 'ga_test', '3.0');
    return ga;
  }
  return null;
}

