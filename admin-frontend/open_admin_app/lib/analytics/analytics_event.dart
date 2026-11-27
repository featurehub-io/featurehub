//

import 'package:open_admin_app/utils/browser/browser.dart' as browser;

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

  AnalyticsPageView({
    required this.title,
    this.additionalParams = const {},
  }) : super(name: 'page_view');

  @override
  Map<String, dynamic> toJson() {
    final map = {
      'page_title': title,
      if (browser.locationPathname != null)
        'page_path': browser.locationPathname,
      'page_location': browser.locationOrigin,
      ...super.toJson(),
      ...additionalParams,
    };
    return map;
  }
}

class AnalyticsEventWithContext extends AnalyticsEvent {
  final Map<String, dynamic> additionalParams;

  AnalyticsEventWithContext({
    required super.name,
    this.additionalParams = const {},
  });

  @override
  Map<String, dynamic> toJson() {
    final map = {
      ...super.toJson(),
      ...additionalParams,
    };
    return map;
  }
}
