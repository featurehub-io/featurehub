library featurehub_sse_client;

import "dart:async";
import "dart:convert";

import "package:http/http.dart" as http;
import "package:http/src/utils.dart" show encodingForCharset;
import "package:http_parser/http_parser.dart" show MediaType;

import "src/decoder.dart";
import "src/event.dart";

export "src/event.dart";

enum EventSourceReadyState {
  CONNECTING,
  OPEN,
  CLOSED,
}

class EventSourceSubscriptionException extends Event implements Exception {
  int statusCode;
  String message;

  @override
  String get data => "$statusCode: $message";

  EventSourceSubscriptionException(this.statusCode, this.message)
      : super(event: "error");
}

/// An EventSource client that exposes a [Stream] of [Event]s.
class EventSource extends Stream<Event> {
  // interface attributes

  final Uri url;
  final Map headers;

  EventSourceReadyState get readyState => _readyState;

  Stream<Event> get onOpen => this.where((e) => e.event == "open");
  Stream<Event> get onMessage => this.where((e) => e.event == "message");
  Stream<Event> get onError => this.where((e) => e.event == "error");

  // internal attributes

  StreamController<Event> _streamController;
  StreamController<List<int>> _incomingDataController;

  EventSourceReadyState _readyState = EventSourceReadyState.CLOSED;

  http.Client client;
  Duration _retryDelay = const Duration(milliseconds: 3000);
  String _lastEventId;
  EventSourceDecoder _decoder;
  String _body;
  String _method;
  final _openOnlyOnFirstListener;
  final _closeOnLastListener;
  http.ByteStream responseStream;

  /// Create a new EventSource by connecting to the specified url.
  static Future<EventSource> connect(url,
      {http.Client client,
      String lastEventId,
      Map headers,
      String body,
      String method,
      bool openOnlyOnFirstListener,
      bool closeOnLastListener}) async {
    // parameter initialization
    url = url is Uri ? url : Uri.parse(url);
    client = client ?? new http.Client();
    lastEventId = lastEventId ?? "";
    body = body ?? "";
    method = method ?? "GET";
    EventSource es = new EventSource._internal(url, client, lastEventId,
        headers, body, method, openOnlyOnFirstListener, closeOnLastListener);
    if (!es._openOnlyOnFirstListener) {
      await es._start();
    }
    return es;
  }

  EventSource._internal(
      this.url,
      this.client,
      this._lastEventId,
      this.headers,
      this._body,
      this._method,
      bool openOnlyOnFirstStream,
      bool closeOnLastStreamClosing)
      : _openOnlyOnFirstListener = openOnlyOnFirstStream ?? false,
        _closeOnLastListener = closeOnLastStreamClosing ?? false {
    // initialize here so we can close the stream
    _streamController = new StreamController<Event>.broadcast(
        onCancel: () => _lastStreamDisconnected());

    _decoder = new EventSourceDecoder(retryIndicator: _updateRetryDelay);
  }

  // proxy the listen call to the controller's listen call
  @override
  StreamSubscription<Event> listen(void onData(Event event),
      {Function onError, void onDone(), bool cancelOnError}) {
    if (_readyState == EventSourceReadyState.CLOSED &&
        _openOnlyOnFirstListener) {
      _start();
    }

    return _streamController.stream.listen(onData,
        onError: onError, onDone: onDone, cancelOnError: cancelOnError);
  }

  void _lastStreamDisconnected() {
    if (_closeOnLastListener && _incomingDataController != null) {
      // delay until next cycle, cannot disconnect while triggering this event
      Future.delayed(Duration(seconds: 0), () async {
        try {
          await _incomingDataController.close();
        } catch (e) {} // swallow the exception if there is one.
        _incomingDataController = null;
      });
    }
  }

  /// Attempt to start a new connection.
  Future _start() async {
    _readyState = EventSourceReadyState.CONNECTING;
    var request = new http.Request(_method, url);
    request.headers["Cache-Control"] = "no-cache";
    request.headers["Accept"] = "text/event-stream";
    if (_lastEventId?.isNotEmpty ?? false) {
      request.headers["Last-Event-ID"] = _lastEventId;
    }
    if (headers != null) {
      headers.forEach((k, v) {
        request.headers[k] = v;
      });
    }
    request.body = _body;

    var response = await client.send(request);
    if (response.statusCode != 200) {
      // server returned an error
      var bodyBytes = await response.stream.toBytes();
      String body = _encodingForHeaders(response.headers).decode(bodyBytes);
      throw new EventSourceSubscriptionException(response.statusCode, body);
    }
    _readyState = EventSourceReadyState.OPEN;
    // start streaming the data

    // push it through a StreamController so we can close it gracefully
    _incomingDataController = StreamController<List<int>>();

    response.stream.listen((value) {
      _incomingDataController.add(value);
    },
        onError: _retry,
        cancelOnError: true,
        onDone: () => _readyState = EventSourceReadyState.CLOSED);

    _incomingDataController.stream.transform(_decoder).listen((Event event) {
      _streamController.add(event);
      _lastEventId = event.id;
    },
        cancelOnError: true,
        onError: _retry,
        onDone: () => _readyState = EventSourceReadyState.CLOSED);
  }

  /// Retries until a new connection is established. Uses exponential backoff.
  Future _retry(dynamic e) async {
    _readyState = EventSourceReadyState.CONNECTING;
    // try reopening with exponential backoff
    Duration backoff = _retryDelay;
    while (true) {
      await new Future.delayed(backoff);
      try {
        await _start();
        break;
      } catch (error) {
        _streamController.addError(error);
        backoff *= 2;
      }
    }
  }

  void _updateRetryDelay(Duration retry) {
    _retryDelay = retry;
  }
}

/// Returns the encoding to use for a response with the given headers. This
/// defaults to [LATIN1] if the headers don't specify a charset or
/// if that charset is unknown.
Encoding _encodingForHeaders(Map<String, String> headers) =>
    encodingForCharset(_contentTypeForHeaders(headers).parameters['charset']);

/// Returns the [MediaType] object for the given headers's content-type.
///
/// Defaults to `application/octet-stream`.
MediaType _contentTypeForHeaders(Map<String, String> headers) {
  var contentType = headers['content-type'];
  if (contentType != null) return new MediaType.parse(contentType);
  return new MediaType("application", "octet-stream");
}
