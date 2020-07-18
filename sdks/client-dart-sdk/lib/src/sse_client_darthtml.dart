import 'dart:async';
import 'dart:convert';
import 'dart:html';

import 'package:featurehub_client_api/api.dart';
import 'package:logging/logging.dart';

import 'repository.dart';

final _log = Logger('featurehub_io_eventsource');

class EventSourceRepositoryListener {
  final ClientFeatureRepository _repository;
  StreamSubscription<Event> _subscription;
  final String url;

  EventSourceRepositoryListener(this.url, ClientFeatureRepository repository,
      {bool doInit = true})
      : _repository = repository {
    if (doInit ?? true) {
      init();
    }
  }

  void _done() {
    _repository.notify(SSEResultState.bye, null);
  }

  void _error(event) {
    _log.severe('Lost connection to feature repository ${event ?? 'unknown'}');
  }

  void _msg(MessageEvent msg) {
    _log.fine('Event is ${msg.type} value ${msg.data}');
    if (msg.type != null) {
      _repository.notify(SSEResultStateTypeTransformer.fromJson(msg.type),
          msg.data == null ? null : jsonDecode(msg.data));
    }
  }

  Future<void> init() async {
    _log.fine('Connecting to $url');

    final es = connect(url)
      ..onError.listen(_error, cancelOnError: true, onDone: _done)
      ..onOpen.listen((event) {
        print("open: " + event.type);
      });

    EventStreamProvider<MessageEvent>('features').forTarget(es).listen(_msg);
    EventStreamProvider<MessageEvent>('feature').forTarget(es).listen(_msg);
    EventStreamProvider<MessageEvent>('bye').forTarget(es).listen(_msg);
    EventStreamProvider<MessageEvent>('failed').forTarget(es).listen(_msg);
    EventStreamProvider<MessageEvent>('ack').forTarget(es).listen(_msg);
    EventStreamProvider<MessageEvent>('delete_feature')
        .forTarget(es)
        .listen(_msg);
  }

  EventSource connect(String url) {
    return EventSource(url);
  }

  void close() {
    if (_subscription != null) {
      _subscription.cancel();
      _subscription = null;
    }
  }
}
