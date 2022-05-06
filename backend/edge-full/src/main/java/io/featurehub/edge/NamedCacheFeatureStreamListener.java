package io.featurehub.edge;

import io.nats.client.Dispatcher;
import io.prometheus.client.Gauge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * When an SSE Client joins, we start listening to the stream of feature updates for that named
 * cache so if they match the right environment for that user, they will get updates published to
 * them.
 */
class NamedCacheFeatureStreamListener {
  private static final Logger log = LoggerFactory.getLogger(NamedCacheFeatureStreamListener.class);
  private int listenerCount;
  private final String subject;
  private final String cacheName;
  private final Dispatcher dispatcher;

  private static final Map<String, Gauge> cacheGauge = new HashMap<>();

  private void incGauge() {
    cacheGauge.computeIfAbsent(cacheName, (cn) -> Gauge.build(gaugeName(),
      "Edge Number of listeners for cache " + cacheName).register()).inc();
  }

  private void decGauge() {
    cacheGauge.get(cacheName).dec();
  }

  private String gaugeName() {
    return "edge_cache_sse_listener_count_" + cacheName.replace(" ", "_").replace("-", "_");
  }

  synchronized void inc() {
    listenerCount++;
    incGauge();
  }

  synchronized void dec(Map<String, NamedCacheFeatureStreamListener> cacheListeners) {
    listenerCount--;
    decGauge();
    if (listenerCount == 0) {
      dispatcher.unsubscribe(subject);
      cacheListeners.remove(subject);
      log.info("no longer listening for named cache `{}` (total: {})", subject, listenerCount);
    }
  }

  public NamedCacheFeatureStreamListener(String subject, String cacheName, Dispatcher dispatcher) {
    this.subject = subject;
    this.cacheName = cacheName;
    this.dispatcher = dispatcher.subscribe(subject);
    listenerCount = 0;

    log.info("listening for feature updates from named cache `{}`", subject);
  }

  public void shutdown() {
    listenerCount = 0;
    dispatcher.unsubscribe(subject);
  }
}
