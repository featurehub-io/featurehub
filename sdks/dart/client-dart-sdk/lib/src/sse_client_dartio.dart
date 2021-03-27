import 'dart:async';
import 'dart:convert';

import 'package:featurehub_client_api/api.dart';
import 'package:featurehub_sse_client/featurehub_sse_client.dart';
import 'package:logging/logging.dart';

import 'repository.dart';

final _log = Logger('featurehub_io_eventsource');

/// This listener will stop if we receive a failed message.
class EventSourceRepositoryListener {
  final ClientFeatureRepository _repository;
  StreamSubscription<Event>? _subscription;
  final String url;
  bool _initialized = false;
  bool _closed = false;
  String? _xFeaturehubHeader;

  EventSourceRepositoryListener(this.url, ClientFeatureRepository repository,
      {bool? doInit = true})
      : _repository = repository {
    if (doInit ?? true) {
      init();
    }
  }

  Future<void> init() async {
    if (!_initialized) {
      _initialized = true;
      await _repository.clientContext.registerChangeHandler((header) async {
        _xFeaturehubHeader = header;
        if (_subscription != null) {
          retry();
        } else {
          // ignore: unawaited_futures
          _init();
        }
      });
    } else {
      _repository.clientContext
          .build(); // trigger shut and restart via the handler above
    }
  }

  void retry() {
    if (_subscription != null) {
      _subscription!.cancel();
      _subscription = null;

      _init();
    }
  }

  Future<void> _init() async {
    _closed = false;
    _log.fine('Connecting to $url');
    final es = await connect(url);

    _subscription = es.listen((event) {
      _log.fine('Event is ${event.event} value ${event.data}');
      final readyness = _repository.readyness;
      if (event.event != null) {
        _repository.notify(SSEResultStateExtension.fromJson(event.event),
            event.data == null ? null : jsonDecode(event.data!));
      }
      if (event.event == 'bye' && readyness != Readyness.Failed && !_closed) {
        retry();
      }
    }, onError: (e) {
      _repository.notify(SSEResultState.bye, null);
    }, onDone: () {
      if (_repository.readyness != Readyness.Failed && !_closed) {
        _repository.notify(SSEResultState.bye, null);
        retry();
      }
    });
  }

  Future<Stream<Event>> connect(String url) {
    var sourceHeaders = {'content-type': 'application/json'};
    if (_xFeaturehubHeader != null) {
      sourceHeaders['x-featurehub'] = _xFeaturehubHeader!;
    }
    return EventSource.connect(url,
        closeOnLastListener: true, headers: sourceHeaders);
  }

  void close() {
    _closed = true;
    if (_subscription != null) {
      _subscription!.cancel();
      _subscription = null;
    }
  }
}
