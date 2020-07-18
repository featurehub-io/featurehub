part of featurehub.client;

class EventSourceRepositoryListener {
  final ClientFeatureRepository _repository;
  StreamSubscription<Event> _subscription;

  EventSourceRepositoryListener(String url, ClientFeatureRepository repository,
      {bool doInit = true})
      : _repository = repository {
    if (doInit ?? true) {
      init(url);
    }
  }

  Future<void> init(String url) async {
    _log.fine('Connecting to $url');
    final es = await connect(url);

    _subscription = es.listen((event) {
      _log.fine('Event is ${event.event} value ${event.data}');
      if (event.event != null) {
        _repository.notify(SSEResultStateTypeTransformer.fromJson(event.event),
            event.data == null ? null : jsonDecode(event.data));
      }
    }, onError: (e) {
      _repository.notify(SSEResultState.failure, null);
    }, onDone: () {
      _repository.notify(SSEResultState.bye, null);
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
