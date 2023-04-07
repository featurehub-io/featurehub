

import 'analytics_service.dart';
import 'g4_analytics_service_io.dart'
  if (dart.library.html) 'g4_analytics_service_web.dart';

/// An umbrella class over platform implementations of Google Analytics 4.
abstract class G4AnalyticsService extends AnalyticsService {
  factory G4AnalyticsService({
    required String measurementId,
  }) =>
    createGoogleAnalytics4Service(measurementId: measurementId); // platform specific func

  /// Since we use the default constructor as the factory,
  /// a non-factory constructor with any other name is required for subclasses.
  G4AnalyticsService.create();
}
