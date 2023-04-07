import 'dart:html';

import 'package:logging/logging.dart';
import 'package:open_admin_app/analytics/analytics_event.dart';
import 'package:open_admin_app/analytics/g4_analytics_service.dart';

import '../analytics/analytics_service.dart';

var _log = Logger('g4');

class FHAnalytics {
  static AnalyticsService? ga;

  static sendScreenView(String viewName, {Map<String, String>? parameters}) {
    ga?.send(AnalyticsPageView(title: viewName, additionalParams: parameters ?? const {}));
  }

  static setGA(String? id) {
    if (id?.trim().isNotEmpty == true && ga == null) {
      _log.fine('received g4 tracking is as $id - initializing');
      ga = G4AnalyticsService(measurementId: id!);
    }
  }
}

