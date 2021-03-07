library featurehub_sse_client.src.decoder;

import "dart:async";
import "dart:convert";

import "event.dart";

typedef RetryIndicator = void Function(Duration retry);

class EventSourceDecoder implements StreamTransformer<List<int>, Event> {
  RetryIndicator? retryIndicator;

  EventSourceDecoder({this.retryIndicator});

  Stream<Event> bind(Stream<List<int>> stream) {
    late StreamController<Event> controller;
    controller = new StreamController(onListen: () {
      // the event we are currently building
      Event currentEvent = new Event();
      // the regexes we will use later
      RegExp lineRegex = new RegExp(r"^([^:]*)(?::)?(?: )?(.*)?$");
      RegExp removeEndingNewlineRegex = new RegExp(r"^((?:.|\n)*)\n$");
      // This stream will receive chunks of data that is not necessarily a
      // single event. So we build events on the fly and broadcast the event as
      // soon as we encounter a double newline, then we start a new one.
      stream
          .transform(new Utf8Decoder())
          .transform(new LineSplitter())
          .listen((String line) {
        if (line.isEmpty) {
          // event is done
          // strip ending newline from data
          if (currentEvent.data != null) {
            var match = removeEndingNewlineRegex.firstMatch(currentEvent.data!)!;
            currentEvent.data = match.group(1);
          }
          controller.add(currentEvent);
          currentEvent = new Event();
          return;
        }
        // match the line prefix and the value using the regex
        Match match = lineRegex.firstMatch(line)!;
        String field = match.group(1)!;
        String value = match.group(2) ?? "";
        if (field.isEmpty) {
          // lines starting with a colon are to be ignored
          return;
        }
        switch (field) {
          case "event":
            currentEvent.event = value;
            break;
          case "data":
            currentEvent.data = (currentEvent.data ?? "") + value + "\n";
            break;
          case "id":
            currentEvent.id = value;
            break;
          case "retry":
            if (retryIndicator != null) {
              retryIndicator!(new Duration(milliseconds: int.parse(value)));
            }
            break;
        }
      });
    });
    return controller.stream;
  }

  StreamTransformer<RS, RT> cast<RS, RT>() =>
      StreamTransformer.castFrom<List<int>, Event, RS, RT>(this);
}
