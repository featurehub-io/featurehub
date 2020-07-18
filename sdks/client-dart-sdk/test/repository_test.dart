import 'dart:async';
import 'dart:convert';

import 'package:featurehub_client_api/api.dart';
import 'package:featurehub_client_sdk/featurehub.dart';
import 'package:test/test.dart';

void main() {
  ClientFeatureRepository repo;

  setUp(() {
    repo = ClientFeatureRepository();
  });

  dynamic _initialFeatures(
      {int version = 1,
      FeatureValueType type = FeatureValueType.BOOLEAN,
      dynamic value = false}) {
    return LocalApiClient.serialize([
      FeatureState()
        ..id = '1'
        ..key = '1'
        ..type = type
        ..version = version
        ..value = value
    ]);
  }

  test('Readyness should fire when features appear', () {
    final sub = repo.readynessStream;

    repo.notify(SSEResultState.features, _initialFeatures());

    expect(sub, emits(Readyness.Ready));
  });

  test('Failure in the stream should indicate failure', () {
    final sub = repo.readynessStream;
    repo.notify(SSEResultState.failure, null);
    expect(sub, emits(Readyness.Failed));
  });

  test(
      'Sending new versions of features into the  repository will trigger new features hook',
      () async {
    repo.notify(SSEResultState.features, _initialFeatures());
    // ignore: unawaited_futures
    expectLater(repo.newFeatureStateAvailableStream, emits(repo));
    repo.notify(SSEResultState.features, _initialFeatures(version: 2));
  });

  test(
      "Sending the same features into the repository won't trigger the new features hook ",
      () {
    repo.notify(SSEResultState.features, _initialFeatures());
    // can't actually test this, it times out, which is actually correct
    expectLater(repo.newFeatureStateAvailableStream, neverEmits(repo),
        skip: true);
    repo.notify(SSEResultState.features, _initialFeatures());
  });

  test(
      "Listening for a feature that doesn't exist and then filling in features triggers stream",
      () {
    final sub = repo.getFeatureState('1').featureUpdateStream;
    sub.listen(expectAsync1((h) => expect(h.booleanValue, equals(false))));
    repo.notify(SSEResultState.features, _initialFeatures());
  });

  test('Listen for string value works as expected', () {
    final sub = repo.getFeatureState('1').featureUpdateStream;
    sub.listen(expectAsync1((h) {
      expect(h.booleanValue, isNull);
      expect(h.stringValue, equals('wopchick'));
    }));
    repo.notify(SSEResultState.features,
        _initialFeatures(type: FeatureValueType.STRING, value: 'wopchick'));
  });

  test('Listen for number value works as expected', () {
    final sub = repo.getFeatureState('1').featureUpdateStream;
    sub.listen(expectAsync1((h) {
      expect(h.booleanValue, isNull);
      expect(h.stringValue, isNull);
      expect(h.numberValue, equals(11.4));
    }));
    repo.notify(SSEResultState.features,
        _initialFeatures(type: FeatureValueType.NUMBER, value: 11.4));
  });

  test('Listen for json value works as expected', () {
    final sub = repo.getFeatureState('1').featureUpdateStream;
    final json = {'fish': 'hello'};
    sub.listen(expectAsync1((h) {
      expect(h.booleanValue, isNull);
      expect(h.stringValue, equals(jsonEncode(json)));
      expect(h.numberValue, isNull);
      expect(h.jsonValue, equals(json));
    }));
    repo.notify(SSEResultState.features,
        _initialFeatures(type: FeatureValueType.JSON, value: jsonEncode(json)));
  });

  test('Features trigger only on change', () {
    final sub = repo.getFeatureState('1').featureUpdateStream;
    // should emit twice and shut down, even if we filled with features three times
    expectLater(
        sub, emitsInOrder([emits(anything), emits(anything), emitsDone]));
    repo.notify(SSEResultState.features, _initialFeatures());
    repo.notify(SSEResultState.features, _initialFeatures());
    repo.notify(
        SSEResultState.features, _initialFeatures(version: 2, value: true));
    repo.shutdown();
  });

  test('Features and then feature trigger change', () {
    final sub = repo.getFeatureState('1').featureUpdateStream;
    // should emit twice and shut down, even if we filled with features three times
    expectLater(
        sub, emitsInOrder([emits(anything), emits(anything), emitsDone]));
    repo.notify(SSEResultState.features, _initialFeatures());
    final data = FeatureState()
      ..version = 2
      ..id = '1'
      ..key = '1'
      ..value = true
      ..type = FeatureValueType.BOOLEAN;
    repo.notify(SSEResultState.feature, data.toJson());
    expect(repo.getFeatureState('1').exists, equals(true));
    repo.shutdown();
  });

  test('New feature value with no state change doesnt trigger change', () {
    final sub = repo.getFeatureState('1').featureUpdateStream;
    // should emit twice and shut down, even if we filled with features three times
    expectLater(sub, emitsInOrder([emits(anything), emitsDone]));
    repo.notify(SSEResultState.features, _initialFeatures());
    final data = FeatureState()
      ..version = 2
      ..id = '1'
      ..key = '1'
      ..value = false
      ..type = FeatureValueType.BOOLEAN;
    repo.notify(SSEResultState.feature, data.toJson());
    repo.shutdown();
  });

  test('Null value indicates exists is false', () {
    repo.notify(SSEResultState.features, _initialFeatures(value: null));
    expect(repo.getFeatureState('1').exists, equals(false));
  });

  test('Getting a copy and then changing value of feature does not change copy',
      () {
    repo.notify(SSEResultState.features, _initialFeatures());
    final copy = repo.getFeatureState('1').copy();
    var data = FeatureState()
      ..version = 2
      ..id = '1'
      ..key = '1'
      ..value = true
      ..type = FeatureValueType.BOOLEAN;
    repo.notify(SSEResultState.feature, data.toJson());
    expect(copy.booleanValue, equals(false));
    expect(repo.getFeatureState('1').booleanValue, equals(true));
  });

  test(
      'Sending bye when not in catch and release will trigger a non ready state',
      () {
    final sub = repo.readynessStream;
    expectLater(
        sub,
        emitsInOrder([
          emits(Readyness.NotReady),
          emits(Readyness.Ready),
          emits(Readyness.NotReady),
          emitsDone
        ]));
    repo.notify(SSEResultState.features, _initialFeatures());
    repo.notify(SSEResultState.bye, null);
    repo.shutdown();
    scheduleMicrotask(() {}); // one for each state
    scheduleMicrotask(() {});
  });

  test(
      'We get initial events for features but once catch and release is enabled, it stops until we release.',
      () {
    final sub = repo.newFeatureStateAvailableStream;
    expect(repo.readyness, equals(Readyness.NotReady));
    repo.notify(SSEResultState.features, _initialFeatures());
    expect(repo.readyness, equals(Readyness.Ready));
    repo.catchAndReleaseMode = true;
    expectLater(repo.catchAndReleaseMode, true);
    expect(repo.getFeatureState('1').booleanValue, equals(false));
    expectLater(sub, emits(repo));
    repo.notify(
        SSEResultState.features, _initialFeatures(version: 2, value: true));
    // now update just the feature
    final data = FeatureState()
      ..version = 3
      ..id = '1'
      ..key = '1'
      ..value = true
      ..type = FeatureValueType.BOOLEAN;

    repo.notify(SSEResultState.feature, data.toJson());

    expect(repo.getFeatureState('1').booleanValue, equals(false));
    expect(repo.getFeatureState('1').type, equals(FeatureValueType.BOOLEAN));
    expect(repo.getFeatureState('1').version, equals(1));
    repo.release();
    expect(repo.getFeatureState('1').booleanValue, equals(true));
    expect(repo.getFeatureState('1').version, equals(3));
    repo.shutdown();
  });

  test('if we delete a feature it is no longer there', () {
    repo.notify(SSEResultState.features, _initialFeatures());
    expect(repo.availableFeatures, contains('1'));
    expect(repo.getFeatureState('1').booleanValue, equals(false));
    expect(repo.getFeatureState('1').value, equals(false));
    final data = FeatureState()
      ..version = 2
      ..id = '1'
      ..key = '1'
      ..value = true
      ..type = FeatureValueType.BOOLEAN;
    repo.notify(SSEResultState.delete_feature, data.toJson());
    expect(repo.getFeatureState('1').exists, isFalse);
  });

  test(
      'if we subscribe to the analytics events we get a copy of the list of current features',
      () {
    repo.notify(SSEResultState.features, _initialFeatures());
    repo.analyticsEvent.listen(expectAsync1((e) {
      expect(e.action, equals('fred'));
      expect(e.other['half'], equals(1.0));
      expect(e.features.length, equals(1));
      expect(e.features[0].key, equals('1'));
    }));
    repo.logAnalyticsEvent('fred', other: {'half': 1.0});
  });
}
