package io.featurehub.edge;

import cd.connect.app.config.ConfigKey;
import cd.connect.app.config.DeclaredConfigResolver;
import cd.connect.lifecycle.ApplicationLifecycleManager;
import cd.connect.lifecycle.LifecycleStatus;
import io.featurehub.jersey.config.CacheJsonMapper;
import io.featurehub.edge.client.ClientConnection;
import io.featurehub.edge.features.DachaFeatureRequestSubmitter;
import io.featurehub.edge.features.FeatureRequestResponse;
import io.featurehub.mr.model.FeatureValueCacheItem;
import io.featurehub.publish.ChannelNames;
import io.featurehub.publish.NATSSource;
import io.nats.client.Dispatcher;
import io.nats.client.Message;
import io.prometheus.client.Gauge;
import jakarta.inject.Inject;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class StreamingFeatureSource implements StreamingFeatureController {
  private static final Logger log = LoggerFactory.getLogger(StreamingFeatureSource.class);
  private final DachaFeatureRequestSubmitter dachaFeatureRequestSubmitter;
  private final NATSSource natsSource;

  private final ExecutorService updateExecutor;
  private final ExecutorService listenExecutor;

  @ConfigKey("update.pool-size")
  Integer updatePoolSize = 10;

  @ConfigKey("listen.pool-size")
  Integer listenPoolSize = 10;

  @ConfigKey("edge.pre-listen-cache-names")
  List<String> cacheNameToPreStartListeningOn = Collections.singletonList("default");

  // environmentId, list of connections for that environment
  private final Map<UUID, Collection<ClientConnection>> notifyOnIncomingFeatureUpdate = new ConcurrentHashMap<>();
  // dispatcher subject based on named-cache, NamedCacheListener
  private final Map<String, NamedCacheFeatureStreamListener> cacheListeners =
      new ConcurrentHashMap<>();

  private final Gauge environmentListenerGauge = Gauge.build("edge_envs_sse_listeners", "Unique " +
    "environments with active sse listeners").register();

  @Inject
  public StreamingFeatureSource(DachaFeatureRequestSubmitter dachaFeatureRequestSubmitter, NATSSource natsSource) {
    this.dachaFeatureRequestSubmitter = dachaFeatureRequestSubmitter;
    this.natsSource = natsSource;

    DeclaredConfigResolver.resolve(this);

    updateExecutor = Executors.newFixedThreadPool(updatePoolSize);
    listenExecutor = Executors.newFixedThreadPool(listenPoolSize);

    log.info("connected to NATS with cache pool size of `{}", updatePoolSize);

    ApplicationLifecycleManager.registerListener(
        trans -> {
          if (trans.next == LifecycleStatus.TERMINATING) {
            shutdown();
          }
        });

    log.info("Pre-listening to named caches: {}", cacheNameToPreStartListeningOn);
    cacheNameToPreStartListeningOn.forEach(this::listenForFeatureUpdates);
  }

  // unsubscribe all and any listeners
  private void shutdown() {
    cacheListeners.values().parallelStream().forEach(NamedCacheFeatureStreamListener::shutdown);
  }

  public void listenForFeatureUpdates(String namedCache) {
    cacheListeners
        .computeIfAbsent(
            namedCacheFeatureListeningChannelSubject(namedCache),
            nc -> {
              Dispatcher dispatcher =
                  natsSource.getConnection().createDispatcher(this::updateFeature);

              return new NamedCacheFeatureStreamListener(nc, namedCache, dispatcher);
            })
        .inc();
  }

  public void unlistenForFeatureUpdates(String namedCache) {
    final NamedCacheFeatureStreamListener ncl =
        cacheListeners.get(namedCacheFeatureListeningChannelSubject(namedCache));

    if (ncl != null) {
      ncl.dec(cacheListeners);
    }
  }

  @NotNull
  private String namedCacheFeatureListeningChannelSubject(String namedCache) {
    return ChannelNames.featureValueChannel(namedCache);
  }

  /**
   * this tells the clients that there are messages for them
   */
  private void updateFeature(Message msg) {
    try {
      FeatureValueCacheItem fv =
          CacheJsonMapper.readFromZipBytes(msg.getData(), FeatureValueCacheItem.class);

      final Collection<ClientConnection> tbc = notifyOnIncomingFeatureUpdate.get(fv.getEnvironmentId());
      if (tbc != null) {
        for (ClientConnection b : tbc) {
          updateExecutor.submit(() -> b.notifyFeature(fv));
        }
      }
    } catch (IOException e) {
      log.warn("unable process incoming feature change.");
    }
  }


  // keep track of expired one so we can walk through periodically and delete them
  //  private Map<String, InflightSdkUrlRequest> expired

  public void requestFeatures(final ClientConnection client) {
    Collection<ClientConnection> clientConnections = null;

    synchronized (notifyOnIncomingFeatureUpdate) {
      clientConnections =
          notifyOnIncomingFeatureUpdate.computeIfAbsent(
              client.getEnvironmentId(), (k) -> {
              environmentListenerGauge.inc();
              return new ConcurrentLinkedQueue<>();
            });
    }

    clientConnections.add(client);

    final KeyParts key = client.getKey();

    listenExecutor.submit(
      () -> {
        try {
          listenForFeatureUpdates(key.getCacheName());

          // this squashes all of the requests and optimises the daylights out of this call so we don't need to
          final List<FeatureRequestResponse> request =
            dachaFeatureRequestSubmitter.request(Collections.singletonList(key),
              client.getClientContext());

          client.initResponse(request.get(0));
          client.registerEjection(this::clientRemoved);
        } catch (Exception nfe) {
          client.failed("unable to communicate with named cache.");
          unlistenForFeatureUpdates(key.getCacheName());
          clientRemoved(client);
        }
      });
  }

  // responsible for removing a client connection once it has been closed
  // from the list of clients we are notifying about feature changes
  public void clientRemoved(ClientConnection client) {
    Collection<ClientConnection> conns = null;
    boolean removed = false;
    boolean environmentListenerPoolShrunk = false;

    // we have to do this in one operation as it is cleaning up this map which can otherwise
    // get filled with useless data
    synchronized (notifyOnIncomingFeatureUpdate) {
      conns = notifyOnIncomingFeatureUpdate.get(client.getEnvironmentId());

      // we are about to remove it, so clean it up
      if (conns != null) {
        removed = conns.remove(client);

        if (conns.isEmpty()) { // make sure the one removed was the last one
          environmentListenerPoolShrunk = notifyOnIncomingFeatureUpdate.remove(client.getEnvironmentId()) != null;
        }
      }
    }

    if (removed) {
      unlistenForFeatureUpdates(client.getNamedCache());
    }

    if (environmentListenerPoolShrunk) {
      environmentListenerGauge.dec();
    }
  }

  @Override
  public void listenExecutor(Runnable runnable) {
    listenExecutor.execute(runnable);
  }
}
