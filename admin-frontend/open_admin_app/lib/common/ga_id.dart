import 'dart:html';

import 'package:usage/usage_html.dart';


class FHAnalytics {
  static Analytics? ga;

  static sendWindowPath() {
    var path = window.location.pathname;
    if (path != null) {
      ga?.sendScreenView(path);
    }
  }

  static sendScreenView(String viewName, {Map<String, String>? parameters}) {
    ga?.sendScreenView(viewName);
  }

  static setGA(String? id) {
    if (id?.trim().isNotEmpty ==  true) {
      ga = AnalyticsHtml(id!, 'ga_test', '3.0');
    }
  }
}

