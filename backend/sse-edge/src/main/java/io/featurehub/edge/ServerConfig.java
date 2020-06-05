package io.featurehub.edge;

import cd.connect.app.config.ConfigKey;
import cd.connect.app.config.DeclaredConfigResolver;
import cd.connect.lifecycle.ApplicationLifecycleManager;
import cd.connect.lifecycle.LifecycleStatus;
import io.featurehub.dacha.api.CacheJsonMapper;
import io.featurehub.edge.client.TimedBucketClientConnection;
import io.featurehub.mr.model.EdgeInitRequest;
import io.featurehub.mr.model.EdgeInitResponse;
import io.featurehub.mr.model.FeatureValueCacheItem;
import io.featurehub.publish.ChannelConstants;
import io.featurehub.publish.ChannelNames;
import io.nats.client.Connection;
import io.nats.client.Dispatcher;
import io.nats.client.Message;
import io.nats.client.Nats;
import io.nats.client.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

class NamedCacheListener {
  private static final Logger log = LoggerFactory.getLogger(NamedCacheListener.class);
  private int listenerCount;
  private String subject;
  Dispatcher dispatcher;

  synchronized void inc() {
    listenerCount ++;
    log.debug("Another client for named cache `{}` (total: {})", subject, listenerCount);
  }

  synchronized void dec(Map<String, NamedCacheListener> cacheListeners) {
    listenerCount --;

    if (listenerCount == 0) {
      dispatcher.unsubscribe(subject);
      cacheListeners.remove(subject);
      log.info("no longer listening for named cache `{}` (total: {})", subject, listenerCount);
    }
  }

  public NamedCacheListener(String subject, Dispatcher dispatcher) {
    this.subject = subject;
    this.dispatcher = dispatcher.subscribe(subject);
    listenerCount = 1;

    log.info("listening for feature updates from named cache `{}`", subject);
  }

  public void shutdown() {
    listenerCount = 0;
    dispatcher.unsubscribe(subject);
  }
}

public class ServerConfig {
  private static final Logger log = LoggerFactory.getLogger(ServerConfig.class);
  @ConfigKey("nats.urls")
  public String natsServer = "nats://localhost:4222";
  private ExecutorService executor;
  @ConfigKey("cache.pool-size")
  Integer cachePoolSize = 10;
  private Connection connection;
  // environmentId, list of connections for that environment
  private Map<String, List<TimedBucketClientConnection>> clientBuckets = new ConcurrentHashMap<>();
  // dispatcher subject based on named-cache, NamedCacheListener
  private Map<String, NamedCacheListener> cacheListeners = new ConcurrentHashMap<>();

  public ServerConfig() {
    DeclaredConfigResolver.resolve(this);

    Options options = new Options.Builder().server(natsServer).build();
    try {
      connection = Nats.connect(options);
    } catch (IOException |InterruptedException e) {
      // should fail if we can't connect
      throw new RuntimeException(e);
    }

    executor = Executors.newFixedThreadPool(cachePoolSize);

    log.info("connected to NATS on `{}` with cache pool size of `{}", natsServer, cachePoolSize);

    ApplicationLifecycleManager.registerListener(trans -> {
      if (trans.next == LifecycleStatus.TERMINATING) {
        shutdown();
      }
    });
  }

  // unsubscribe all and any listeners
  private void shutdown() {
    cacheListeners.values().parallelStream().forEach(NamedCacheListener::shutdown);
  }

  void listenForFeatureUpdates(String namedCache) {
    String subject = ChannelNames.featureValueChannel(namedCache);

    cacheListeners.computeIfAbsent(subject, nc -> {
      Dispatcher dispatcher = connection.createDispatcher(this::updateFeature);

      return new NamedCacheListener(nc, dispatcher);
    });
  }

  public void unlistenForFeatureUpdates(String namedCache) {
    String subject = ChannelNames.featureValueChannel(namedCache);
    final NamedCacheListener ncl = cacheListeners.get(subject);

    if (ncl != null) {
      ncl.dec(cacheListeners);
    }
  }


  private void updateFeature(Message msg) {
    try {
      FeatureValueCacheItem fv = CacheJsonMapper.mapper.readValue(msg.getData(), FeatureValueCacheItem.class);

      final List<TimedBucketClientConnection> tbc = clientBuckets.get(fv.getEnvironmentId());
      if (tbc != null) {
        tbc.forEach(b -> {
          executor.submit(() -> {
            b.notifyFeature(fv);
          });
        });
      }
    } catch (IOException e) {
      log.warn("unable process incoming feature change.");
    }
  }

  public void requestFeatures(final TimedBucketClientConnection client) {
    clientBuckets.computeIfAbsent(client.getEnvironmentId(), (k) -> new ArrayList<>()).add(client);

    client.registerEjection(this::clientRemoved);

    executor.submit(() -> {
      EdgeInitRequest request = new EdgeInitRequest().apiKey(client.getApiKey()).environmentId(client.getEnvironmentId());

      try {
        String subject = client.getNamedCache() + "/" + ChannelConstants.EDGE_CACHE_CHANNEL;

        listenForFeatureUpdates(client.getNamedCache());

        Message response = connection.request(subject, CacheJsonMapper.mapper.writeValueAsBytes(request), Duration.ofMillis(2000));

        if (response != null) {
          EdgeInitResponse edgeResponse = CacheJsonMapper.mapper.readValue(response.getData(), EdgeInitResponse.class);

          client.initResponse(edgeResponse);

          // if they had no access or it doesn't exist
          if (!edgeResponse.getSuccess()) {
            unlistenForFeatureUpdates(client.getNamedCache());
          }
        } else {
          client.failed("unable to communicate with named cache.");
        }
      } catch (Exception e) {
        client.failed("unable to communicate with named cache.");
      }
    });
  }

  // responsible for removing a client connection once it has been closed
  // from the list of clients we are notifying about feature changes
  private void clientRemoved(TimedBucketClientConnection client) {
    final List<TimedBucketClientConnection> conns = clientBuckets.get(client.getEnvironmentId());

    if (conns != null) {
      conns.remove(client);
    }
  }
}
