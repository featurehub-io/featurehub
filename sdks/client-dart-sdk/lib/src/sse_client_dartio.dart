import 'dart:async';
import 'dart:convert';

import 'package:eventsource/eventsource.dart';
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

  void retry() {
    if (_subscription != null) {
      _subscription.cancel();
      _subscription = null;
    }

    init();
  }

  Future<void> init() async {
    _log.fine('Connecting to $url');
    final es = await connect(url);

    _subscription = es.listen((event) {
      _log.fine('Event is ${event.event} value ${event.data}');
      if (event.event != null) {
        _repository.notify(SSEResultStateTypeTransformer.fromJson(event.event),
            event.data == null ? null : jsonDecode(event.data));
      }
      if (event.event == 'bye') {
        retry();
      }
    }, onError: (e) {
      _repository.notify(SSEResultState.failure, null);
    }, onDone: () {
      _repository.notify(SSEResultState.bye, null);
      retry();
    });
  }

  Future<Stream<Event>> connect(String url) {
    return EventSource.connect(url,
        closeOnLastListener: true,
        headers: {'content-type': 'application/json'});
  }

  void close() {
    if (_subscription != null) {
      _subscription.cancel();
      _subscription = null;
    }
  }
}
