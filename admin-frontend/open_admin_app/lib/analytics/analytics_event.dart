//



import 'dart:html'; // ignore: avoid_web_libraries_in_flutter

abstract class AnalyticsEvent {
  const AnalyticsEvent({
    required this.name,
  });

  final String name;

  /// Returns the event params to be sent.
  Map<String, dynamic> toJson() => const {};
}

class AnalyticsPageView extends AnalyticsEvent {
  final Map<String, dynamic> additionalParams;
  final String title;

  AnalyticsPageView({required this.title, this.additionalParams = const {},}) : super(name: 'page_view');

  @override
  Map<String, dynamic> toJson() {
    final map = {
      'page_title': title,
      if (window.location.pathname != null)
        'page_path': window.location.pathname,
      if (window.location.origin != null)
        'page_location': window.location.origin,
      ...super.toJson(),
      ...additionalParams,
    };
    return map;
  }
}

class AnalyticsEventWithContext extends AnalyticsEvent {
  final Map<String, dynamic> additionalParams;

  AnalyticsEventWithContext({required String name, this.additionalParams = const {},}) : super(name: name);

  @override
  Map<String, dynamic> toJson() {
    final map = {
      ...super.toJson(),
      ...additionalParams,
    };
    return map;
  }
}
