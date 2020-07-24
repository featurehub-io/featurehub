import 'package:featurehub_sse_client/featurehub_sse_client.dart';

main() async {
  // Because EventSource uses the http package, all platforms for which http
  // works, will be able to use the generic method:

  EventSource eventSource =
      await EventSource.connect("http://example.org/events");
  // listen for events
  eventSource.listen((Event event) {
    print("New event:");
    print("  event: ${event.event}");
    print("  data: ${event.data}");
  });

  // If you know the last event.id from a previous connection, you can try this:

  String lastId = "iknowmylastid";
  eventSource = await EventSource.connect("http://example.org/events",
      lastEventId: lastId);
  // listen for events
  eventSource.listen((Event event) {
    print("New event:");
    print("  event: ${event.event}");
    print("  data: ${event.data}");
  });
}
