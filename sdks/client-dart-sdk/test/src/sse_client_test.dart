import 'dart:async';

import 'package:eventsource/eventsource.dart';
import 'package:featurehub_client_api/api.dart';
import 'package:featurehub_client_sdk/featurehub_io.dart';
import 'package:mockito/mockito.dart';
import 'package:rxdart/rxdart.dart';
import 'package:test/test.dart';

class _MockStreamEvent extends Mock implements Stream<Event> {}

class _MockRepository extends Mock implements ClientFeatureRepository {}

class SseClientTest extends EventSourceRepositoryListener {
  final Stream<Event> mockedSource;

  SseClientTest(
      String url, ClientFeatureRepository repository, Stream<Event> eventSource,
      {bool doInit})
      : mockedSource = eventSource,
        super(url, repository, doInit: doInit);

  @override
  Future<Stream<Event>> connect(String url) async {
    return mockedSource;
  }
}

void main() {
  PublishSubject<Event> es;
  _MockRepository rep;
//  SseClientTest sse;

  setUp(() {
    es = PublishSubject<Event>();
    rep = _MockRepository();

    SseClientTest('', rep, es);
  });

  test('A proper message is delivered to the repository', () {
    es.listen(expectAsync1((_) {
      verify(rep.notify(SSEResultState.failure, any));
    }));

    es.add(Event(
        event: SSEResultStateTypeTransformer.toJson(SSEResultState.failure),
        data: '{}'));
  });

  test('A failure is reported to the repository', () {
    es.listen((value) {}, onError: expectAsync1((_) {
      verify(rep.notify(SSEResultState.failure, any));
    }));
    es.addError('blah');
  });

  test('Closure of stream is reported to repository', () {
    es.listen((value) {}, onDone: expectAsync0(() {
      verify(rep.notify(SSEResultState.bye, any));
    }));

    es.close();
  });

  test('Closing unlistens to the stream', () async {
    final stream = _MockStreamEvent();
    final sub = _MockSubscription();
    rep = _MockRepository();

    when(stream.listen(any,
            onError: anyNamed('onError'), onDone: anyNamed('onDone')))
        .thenAnswer((_) => sub);

    final sse = SseClientTest('', rep, stream, doInit: false);
    await sse.init();

    sse.close();

    verify(sub.cancel());
  });
}

class _MockSubscription extends Mock implements StreamSubscription<Event> {}
