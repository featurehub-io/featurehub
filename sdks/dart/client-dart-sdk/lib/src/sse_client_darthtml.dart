import 'dart:async';
import 'dart:convert';
import 'dart:html';

import 'package:featurehub_client_api/api.dart';
import 'package:logging/logging.dart';

import 'repository.dart';

final _log = Logger('featurehub_io_eventsource');

class EventSourceRepositoryListener {
  final ClientFeatureRepository _repository;
  StreamSubscription<Event>? _subscription;
  final String _url;
  bool _initialized = false;
  String? xFeaturehubHeader;
  EventSource? es;

  EventSourceRepositoryListener(
      String url, String apiKey, ClientFeatureRepository repository,
      {bool doInit = true})
      : _repository = repository,
        _url = url + (url.endsWith('/') ? '' : '/') + 'features/' + apiKey {
    if (apiKey.contains('*')) {
      throw Exception(
          'You are using a client evaluated API Key in Dart and this is not supported.');
    }
    if (doInit) {
      init();
    }
  }

  Future<void> init() async {
    if (!_initialized) {
      _initialized = true;
      await _repository.clientContext.registerChangeHandler((header) async {
        xFeaturehubHeader = header;
        if (es != null) {
          es!.close();
        }
        // ignore: unawaited_futures
        _init();
      });
    } else {
      _repository.clientContext.build();
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
    _repository.notify(SSEResultStateExtension.fromJson(msg.type),
        msg.data == null ? null : jsonDecode(msg.data));
  }

  Future<void> _init() async {
    _log.fine('Connecting to $_url');

    es = connect(_url)
      ..onError.listen(_error, cancelOnError: true, onDone: _done);

    EventStreamProvider<MessageEvent>('features').forTarget(es).listen(_msg);
    EventStreamProvider<MessageEvent>('feature').forTarget(es).listen(_msg);
    EventStreamProvider<MessageEvent>('bye').forTarget(es).listen(_msg);
    EventStreamProvider<MessageEvent>('failed').forTarget(es).listen((e) {
      _msg(e);
      _log.fine('Failed connection to server, disconnecting');
      es!.close();
    });
    EventStreamProvider<MessageEvent>('ack').forTarget(es).listen(_msg);
    EventStreamProvider<MessageEvent>('delete_feature')
        .forTarget(es)
        .listen(_msg);
  }

  EventSource connect(String url) {
    return EventSource(url +
        (xFeaturehubHeader == null ? '' : '?xfeaturehub=$xFeaturehubHeader'));
  }

  void close() {
    if (_subscription != null) {
      _subscription!.cancel();
      _subscription = null;
    }
  }
}
