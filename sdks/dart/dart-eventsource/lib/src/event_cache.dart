library featurehub_sse_client.src.cache;

import "package:collection/collection.dart";

import "event.dart";

//TODO use more efficient data structure than List
class EventCache {
  final int? cacheCapacity;
  final bool comparableIds;
  Map<String, List<Event>> _caches = new Map<String, List<Event>>();

  EventCache({this.cacheCapacity, this.comparableIds: true});

  void replay(Sink<Event> sink, String lastEventId, [String channel = ""]) {
    List<Event>? cache = _caches[channel];
    if (cache == null || cache.isEmpty) {
      // nothing to replay
      return;
    }
    // find the location of lastEventId in the queue
    int index;
    if (comparableIds) {
      // if comparableIds, we can use binary search
      index = binarySearch(cache, lastEventId);
    } else {
      // otherwise, we starts from the last one and look one by one
      index = cache.length - 1;
      while (index > 0 && cache[index].id != lastEventId) {
        index--;
      }
    }
    if (index >= 0) {
      // add them all to the sink
      cache.sublist(index).forEach(sink.add);
    }
  }

  /// Add a new [Event] to the cache(s) of the specified channel(s).
  /// Please note that we assume events are added with increasing values of
  /// [Event.id].
  void add(Event event, [Iterable<String> channels = const [""]]) {
    for (String channel in channels) {
      List<Event> cache = _caches.putIfAbsent(channel, () => []);
      if (cacheCapacity != null && cache.length >= cacheCapacity!) {
        cache.removeAt(0);
      }
      cache.add(event);
    }
  }

  void clear([Iterable<String> channels = const [""]]) {
    channels.forEach(_caches.remove);
  }

  void clearAll() {
    _caches.clear();
  }
}
