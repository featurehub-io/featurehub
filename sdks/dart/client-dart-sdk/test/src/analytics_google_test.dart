import 'package:featurehub_client_api/api.dart';
import 'package:featurehub_client_sdk/featurehub.dart';
import 'package:mockito/mockito.dart';
import 'package:rxdart/rxdart.dart';
import 'package:test/test.dart';

void main() {
  _MockFeatureRepository repo;
  GoogleAnalyticsApiClient poster;

  setUp(() {
    repo = _MockFeatureRepository();
    poster = _MockPost();
  });

  test('we can log an analytics event for a feature', () async {
    GoogleAnalyticsListener(repo, '1234', cid: '123', apiClient: poster);

    final ae = AnalyticsEvent('foo', [_MockStringFeature()], null);

    repo.analyticsCollectors.add(ae);
    await Future.value();

    verify(poster.postAnalyticBatch(
        'v=1&tid=1234&cid=123&t=event&ec=FeatureHub%20Event&ea=foo&el=key+%3A+sval\n'));
  });

  test("when we don't provide a cid, no post happens", () async {
    GoogleAnalyticsListener(repo, '1234', apiClient: poster);

    final ae = AnalyticsEvent('foo', [_MockStringFeature()], null);

    repo.analyticsCollectors.add(ae);
    await Future.value();

    verifyNever(poster.postAnalyticBatch(any));
  });
}

class _MockFeatureRepository extends Mock implements ClientFeatureRepository {
  final analyticsCollectors = PublishSubject<AnalyticsEvent>();
  @override
  Stream<AnalyticsEvent> get analyticsEvent => analyticsCollectors.stream;
}

class _MockPost extends Mock implements GoogleAnalyticsApiClient {}

class _MockStringFeature extends Mock implements FeatureStateHolder {
  @override
  String get stringValue => 'sval';
  @override
  String get key => 'key';
  @override
  FeatureValueType get type => FeatureValueType.STRING;
}
