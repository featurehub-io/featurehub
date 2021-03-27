import 'dart:async';

import 'package:featurehub_client_api/api.dart';
import 'package:featurehub_client_sdk/featurehub.dart'
    hide EventSourceRepositoryListener;
import 'package:featurehub_client_sdk/featurehub_io.dart';
import 'package:featurehub_sse_client/featurehub_sse_client.dart';
import 'package:mockito/mockito.dart';
import 'package:rxdart/rxdart.dart';
import 'package:test/test.dart';

class _MockStreamEvent extends Mock implements Stream<Event> {}

class _MockRepository extends Mock implements ClientFeatureRepository {
  @override
  final ClientContext clientContext = ClientContext();

  Readyness r = Readyness.Ready;

  @override
  Readyness get readyness => r;
}

class SseClientTest extends EventSourceRepositoryListener {
  final Stream<Event> mockedSource;

  SseClientTest(
      String url, ClientFeatureRepository repository, Stream<Event> eventSource,
      {bool doInit = true})
      : mockedSource = eventSource,
        super(url, repository, doInit: doInit);

  @override
  Future<Stream<Event>> connect(String url) async {
    return mockedSource;
  }
}

void main() {
  late PublishSubject<Event> es;
  late _MockRepository rep;
  // SseClientTest sse;

  setUp(() {
    es = PublishSubject<Event>();
    rep = _MockRepository();

    SseClientTest('', rep, es);
  });

  test('A proper message is delivered to the repository', () {
    es.listen(expectAsync1((_) {
      verify(rep.notify(SSEResultState.failure, any));
    }));

    es.add(Event(event: SSEResultState.failure.name, data: '{}'));
  });

  test('A failure is reported to the repository', () {
    rep.r = Readyness.Failed;
    es.listen((value) {}, onError: expectAsync1((dynamic _) {
      verify(rep.notify(SSEResultState.bye, any));
    }));
    es.addError('blah');
  });

  // this one is an endless loop

  // test('Closure of stream is reported to repository', () {
  //   es.listen((value) {}, onDone: expectAsync0(() {
  //     verify(rep.notify(SSEResultState.bye, any));
  //   }));
  //
  //   es.close();
  // });

  test('Closing unlistens to the stream', () async {
    final stream = _MockStreamEvent();
    final sub = _MockSubscription();
    rep = _MockRepository();

    when(stream.listen(any,
            onError: anyNamed('onError'), onDone: anyNamed('onDone')))
        .thenReturn(sub);

    final sse = SseClientTest('', rep, stream, doInit: false);
    await sse.init();

    sse.close();

    verify(sub.cancel());
  });
}

class _MockSubscription extends Mock implements StreamSubscription<Event> {}
