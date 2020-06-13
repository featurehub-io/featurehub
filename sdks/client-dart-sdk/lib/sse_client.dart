part of featurehub.client;

class EventSourceRepositoryListener {
  final ClientFeatureRepository _repository;
  StreamSubscription<Event> _subscription;

  EventSourceRepositoryListener(String url, ClientFeatureRepository repository)
      : _repository = repository {
    _init(url);
  }

  void _init(String url) async {
    final es = await EventSource.connect(url);

    _subscription = es.listen((event) {
      _log.fine('Event is ${event.event} value ${event.data}');
      if (event.event != null) {
        _repository.notify(SSEResultStateTypeTransformer.fromJson(event.event),
            jsonDecode(event.data));
      }
    }, onError: (e) {
      _repository.notify(SSEResultState.failure, null);
    }, onDone: () {
      _repository.notify(SSEResultState.bye, null);
    });
  }

  void close() {
    if (_subscription != null) {
      _subscription.cancel();
      _subscription = null;
    }
  }
}
