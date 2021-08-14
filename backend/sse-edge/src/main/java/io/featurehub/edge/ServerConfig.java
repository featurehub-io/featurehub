package io.featurehub.edge;

import cd.connect.app.config.ConfigKey;
import cd.connect.app.config.DeclaredConfigResolver;
import cd.connect.lifecycle.ApplicationLifecycleManager;
import cd.connect.lifecycle.LifecycleStatus;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.featurehub.dacha.api.CacheJsonMapper;
import io.featurehub.dacha.api.DachaApiKeyService;
import io.featurehub.dacha.api.DachaClientServiceRegistry;
import io.featurehub.edge.client.ClientConnection;
import io.featurehub.mr.messaging.StreamedFeatureUpdate;
import io.featurehub.mr.model.DachaKeyDetailsResponse;
import io.featurehub.mr.model.DachaPermissionResponse;
import io.featurehub.mr.model.FeatureValueCacheItem;
import io.featurehub.publish.ChannelNames;
import io.featurehub.publish.NATSSource;
import io.nats.client.Dispatcher;
import io.nats.client.Message;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

class NamedCacheListener {
  private static final Logger log = LoggerFactory.getLogger(NamedCacheListener.class);
  private int listenerCount;
  private final String subject;
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

public class ServerConfig implements ServerController {
  private static final Logger log = LoggerFactory.getLogger(ServerConfig.class);
  private final DachaClientServiceRegistry dachaClientRegistry;
  private final NATSSource natsSource;

  private final ExecutorService updateExecutor;
  private final ExecutorService listenExecutor;

  @ConfigKey("update.pool-size")
  Integer updatePoolSize = 10;

  @ConfigKey("listen.pool-size")
  Integer listenPoolSize = 10;

  // environmentId, list of connections for that environment
  private final Map<UUID, Collection<ClientConnection>> clientBuckets = new ConcurrentHashMap<>();
  // dispatcher subject based on named-cache, NamedCacheListener
  private final Map<String, NamedCacheListener> cacheListeners = new ConcurrentHashMap<>();
  private final FeatureTransformer featureTransformer = new FeatureTransformerUtils();

  @Inject
  public ServerConfig(DachaClientServiceRegistry dachaClientRegistry, NATSSource natsSource) {
    this.dachaClientRegistry = dachaClientRegistry;
    this.natsSource = natsSource;

    DeclaredConfigResolver.resolve(this);

    updateExecutor = Executors.newFixedThreadPool(updatePoolSize);
    listenExecutor = Executors.newFixedThreadPool(listenPoolSize);

    log.info("connected to NATS with cache pool size of `{}", updatePoolSize);

    ApplicationLifecycleManager.registerListener(trans -> {
      if (trans.next == LifecycleStatus.TERMINATING) {
        shutdown();
      }
    });

    startEmptySSEListenerRequestEjectionTimer();
  }

  // unsubscribe all and any listeners
  private void shutdown() {
    cacheListeners.values().parallelStream().forEach(NamedCacheListener::shutdown);
  }

  @Override
  public void listenForFeatureUpdates(String namedCache) {
    String subject = ChannelNames.featureValueChannel(namedCache);

    cacheListeners.computeIfAbsent(subject, nc -> {
      Dispatcher dispatcher = natsSource.getConnection().createDispatcher(this::updateFeature);

      return new NamedCacheListener(nc, dispatcher);
    });
  }

  @Override
  public void unlistenForFeatureUpdates(String namedCache) {
    String subject = ChannelNames.featureValueChannel(namedCache);
    final NamedCacheListener ncl = cacheListeners.get(subject);

    if (ncl != null) {
      ncl.dec(cacheListeners);
    }
  }

  public void publishFeatureChangeRequest(StreamedFeatureUpdate featureUpdate, String namedCache) {
    // oh for asyncapi being actually useful
    String subject = "/" + namedCache + "/feature-updates";

    try {
      natsSource.getConnection().publish(subject, CacheJsonMapper.mapper.writeValueAsBytes(featureUpdate));
    } catch (JsonProcessingException e) {
      log.error("Unable to send feature-update message to server");
    }
  }


  private void updateFeature(Message msg) {
    try {
      FeatureValueCacheItem fv = CacheJsonMapper.readFromZipBytes(msg.getData(), FeatureValueCacheItem.class);

      final Collection<ClientConnection> tbc = clientBuckets.get(fv.getEnvironmentId());
      if (tbc != null) {
        for (ClientConnection b : tbc) {
          updateExecutor.submit(() -> b.notifyFeature(fv));
        }
      }
    } catch (IOException e) {
      log.warn("unable process incoming feature change.");
    }
  }

  private final Map<KeyParts, InflightSSEListenerRequest> inflightSSEListenerRequests = new ConcurrentHashMap<>();

  // this ensures we don't get an ever growing memory leak of sdk requests that no-one is using
  // if we don't do this, it can lead to an attack that would rob an Edge listener of remaining memory
  // while consuming no services on the client. People can just randomly fire junk at SSE edge and it would
  // leak memory
  protected void startEmptySSEListenerRequestEjectionTimer() {
    TimerTask task = new TimerTask() {
      @Override
      public void run() {
        inflightSSEListenerRequests.values().iterator().forEachRemaining(InflightSSEListenerRequest::removeCheck);
      }
    };

    new Timer().scheduleAtFixedRate(task, 5000, 5000);
  }

  // keep track of expired one so we can walk through periodically and delete them
//  private Map<String, InflightSdkUrlRequest> expired



  public void requestFeatures(final ClientConnection client) {
    final DachaApiKeyService apiKeyService = dachaClientRegistry.getApiKeyService(client.getNamedCache());

    if (apiKeyService == null) {
      client.failed("unable to communicate with named cache.");
      return;
    }

    clientBuckets.computeIfAbsent(client.getEnvironmentId(), (k) -> new ConcurrentLinkedQueue<>()).add(client);

    final KeyParts key = client.getKey();
    final InflightSSEListenerRequest inflightSSEListenerRequest =
      inflightSSEListenerRequests.computeIfAbsent(key, (k) -> new InflightSSEListenerRequest(k, this));

    // if we are the first one, make the request. If any follow before this one finishes it gets this result
    if (inflightSSEListenerRequest.add(client) == 0) {
      listenExecutor.submit(
          () -> {
            try {
              listenForFeatureUpdates(key.getCacheName());

              final DachaKeyDetailsResponse details =
                apiKeyService.getApiKeyDetails(key.getEnvironmentId(), key.getServiceKey());

              copyKeyDetails(key, details);

              inflightSSEListenerRequest.success(details);
            } catch (Exception nfe) {
              inflightSSEListenerRequest.reject();
            }
          });
    }
  }

  public DachaPermissionResponse requestPermission(KeyParts key, String featureKey) {
    final DachaApiKeyService apiKeyService = dachaClientRegistry.getApiKeyService(key.getCacheName());

    if (apiKeyService == null) {
      return null;
    }

    try {
      return apiKeyService.getApiKeyPermissions(key.getEnvironmentId(), key.getServiceKey(), featureKey);
    } catch (Exception ignored) {
      return null;
    }
  }

  // responsible for removing a client connection once it has been closed
  // from the list of clients we are notifying about feature changes
  public void clientRemoved(ClientConnection client) {
    final Collection<ClientConnection> conns = clientBuckets.get(client.getEnvironmentId());

    if (conns != null) {
      conns.remove(client);
    }
  }

  @Override
  public void listenExecutor(Runnable runnable) {
    listenExecutor.execute(runnable);
  }

  @Override
  public void removeInflightSSEListenerRequest(KeyParts key) {
    inflightSSEListenerRequests.remove(key);
  }

  private void copyKeyDetails(KeyParts key, DachaKeyDetailsResponse details) {
    key.setOrganisationId(details.getOrganizationId());
    key.setPortfolioId(details.getPortfolioId());
    key.setApplicationId(details.getApplicationId());
    key.setServiceKeyId(details.getServiceKeyId());
  }
}
