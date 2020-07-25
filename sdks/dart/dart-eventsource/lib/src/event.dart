library featurehub_sse_client.src.event;

class Event implements Comparable<Event> {
  /// An identifier that can be used to allow a client to replay
  /// missed Events by returning the Last-Event-Id header.
  /// Return empty string if not required.
  String id;

  /// The name of the event. Return empty string if not required.
  String event;

  /// The payload of the event.
  String data;

  Event({this.id, this.event, this.data});

  Event.message({this.id, this.data}) : event = "message";

  @override
  int compareTo(Event other) => id.compareTo(other.id);
}
