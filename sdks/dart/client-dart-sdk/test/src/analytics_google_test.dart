import 'package:featurehub_client_api/api.dart';
import 'package:featurehub_client_sdk/featurehub.dart';
import 'package:mockito/mockito.dart';
import 'package:rxdart/rxdart.dart';
import 'package:test/test.dart';

void main() {
  test("we can log an analytics event for a feature", () {
    final repo = _MockFeatureRepository();
    final poster = _MockPost();
    final ga =
        GoogleAnalyticsListener(repo, '1234', cid: '123', apiClient: poster);

    AnalyticsEvent ae = AnalyticsEvent('foo', [_MockStringFeature()], null);

    repo.analyticsCollectors.add(ae);

    verify(poster.postAnalyticBatch(any));
  });
}

class _MockFeatureRepository extends Mock implements ClientFeatureRepository {
  final analyticsCollectors = PublishSubject<AnalyticsEvent>();
  Stream<AnalyticsEvent> get analyticsEvent => analyticsCollectors.stream;
}

class _MockPost extends Mock implements GoogleAnalyticsApiClient {}

class _MockStringFeature extends Mock implements FeatureStateHolder {
  String get stringValue => "sval";
  FeatureValueType get type => FeatureValueType.STRING;
}
