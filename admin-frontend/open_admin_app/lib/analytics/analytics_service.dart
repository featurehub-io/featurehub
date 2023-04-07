
import 'dart:collection';
import 'package:meta/meta.dart';
import 'analytics_event.dart';

AnalyticsService analyticsService = AnalyticsService();

class AnalyticsService {
  AnalyticsEvent? _lastEvent;
  AnalyticsEvent? get lastEvent => _lastEvent;

  final _defaultEventParameters = <String, dynamic>{};

  /// The parameters sent with all events.
  ///
  /// Individual event parameters have higher priority on collisions.
  Map<String, dynamic> get defaultEventParameters =>
      UnmodifiableMapView(_defaultEventParameters);

  set defaultEventParameters(Map<String, dynamic> newValue) {
    _defaultEventParameters.clear();
    _defaultEventParameters.addAll(newValue);
  }

  @protected
  Future<void> sendProtected(AnalyticsEvent event) async {}

  // so we don't need to have to await it and lint it everywhere
  void send(AnalyticsEvent event) {
    _lastEvent = event;
    sendProtected(event);
  }
}
