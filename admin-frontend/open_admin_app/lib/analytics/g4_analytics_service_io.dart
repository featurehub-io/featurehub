

import 'package:meta/meta.dart';
import 'analytics_event.dart';
import 'analytics_service.dart';
import 'g4_analytics_service.dart';

@internal
G4AnalyticsService createGoogleAnalytics4Service({
  required String measurementId,
}) =>
    GoogleAnalytics4ServiceNonWeb();

/// The required placeholder for non-web builds, e.g. unit tests.
class GoogleAnalytics4ServiceNonWeb extends G4AnalyticsService {
  GoogleAnalytics4ServiceNonWeb() : super.create();

  @override
  Future<void> sendProtected(AnalyticsEvent event) {
    throw UnimplementedError();
  }
}
