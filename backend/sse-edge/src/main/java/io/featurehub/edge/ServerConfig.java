package io.featurehub.edge;

import cd.connect.app.config.ConfigKey;
import cd.connect.app.config.DeclaredConfigResolver;
import cd.connect.lifecycle.ApplicationLifecycleManager;
import cd.connect.lifecycle.LifecycleStatus;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.featurehub.dacha.api.CacheJsonMapper;
import io.featurehub.edge.client.ClientConnection;
import io.featurehub.mr.messaging.StreamedFeatureUpdate;
import io.featurehub.mr.model.EdgeInitPermissionResponse;
import io.featurehub.mr.model.EdgeInitRequest;
import io.featurehub.mr.model.EdgeInitRequestCommand;
import io.featurehub.mr.model.EdgeInitResponse;
import io.featurehub.mr.model.FeatureValueCacheItem;
import io.featurehub.publish.ChannelConstants;
import io.featurehub.publish.ChannelNames;
import io.featurehub.sse.model.Environment;
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
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

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
  @ConfigKey("nats.urls")
  public String natsServer = "nats://localhost:4222";
  private ExecutorService updateExecutor;
  private ExecutorService listenExecutor;
  @ConfigKey("update.pool-size")
  Integer updatePoolSize = 10;
  @ConfigKey("listen.pool-size")
  Integer listenPoolSize = 10;
  private Connection connection;
  // environmentId, list of connections for that environment
  private Map<String, Collection<ClientConnection>> clientBuckets = new ConcurrentHashMap<>();
  // dispatcher subject based on named-cache, NamedCacheListener
  private Map<String, NamedCacheListener> cacheListeners = new ConcurrentHashMap<>();
  private FeatureTransformer featureTransformer = new FeatureTransformerUtils();

  public ServerConfig() {
    DeclaredConfigResolver.resolve(this);

    Options options = new Options.Builder().server(natsServer).build();
    try {
      connection = Nats.connect(options);
    } catch (IOException |InterruptedException e) {
      // should fail if we can't connect
      throw new RuntimeException(e);
    }

    updateExecutor = Executors.newFixedThreadPool(updatePoolSize);
    listenExecutor = Executors.newFixedThreadPool(listenPoolSize);

    log.info("connected to NATS on `{}` with cache pool size of `{}", natsServer, updatePoolSize);

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
      Dispatcher dispatcher = connection.createDispatcher(this::updateFeature);

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
      connection.publish(subject, CacheJsonMapper.mapper.writeValueAsBytes(featureUpdate));
    } catch (JsonProcessingException e) {
      log.error("Unable to send feature-update message to server");
    }
  }


  private void updateFeature(Message msg) {
    try {
      FeatureValueCacheItem fv = CacheJsonMapper.mapper.readValue(msg.getData(), FeatureValueCacheItem.class);

      final Collection<ClientConnection> tbc = clientBuckets.get(fv.getEnvironmentId());
      if (tbc != null) {
        tbc.forEach(b -> {
          updateExecutor.submit(() -> {
            b.notifyFeature(fv);
          });
        });
      }
    } catch (IOException e) {
      log.warn("unable process incoming feature change.");
    }
  }

  private final Map<String, InflightSSEListenerRequest> inflightSSEListenerRequests = new ConcurrentHashMap<>();

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
    clientBuckets.computeIfAbsent(client.getEnvironmentId(), (k) -> new ConcurrentLinkedQueue<>()).add(client);

    final String key = client.getApiKey() + "," + client.getEnvironmentId();
    final InflightSSEListenerRequest inflightSSEListenerRequest = inflightSSEListenerRequests.computeIfAbsent(key,
        (k) -> new InflightSSEListenerRequest(key, this));

    // if we are the first one, make the request. If any follow before this one finishes it gets this result
    if (inflightSSEListenerRequest.add(client) == 0) {
      listenExecutor.submit(() -> {
        EdgeInitRequest request = new EdgeInitRequest()
          .command(EdgeInitRequestCommand.LISTEN)
          .apiKey(client.getApiKey())
          .environmentId(client.getEnvironmentId());

        try {
          String subject = client.getNamedCache() + "/" + ChannelConstants.EDGE_CACHE_CHANNEL;

          listenForFeatureUpdates(client.getNamedCache());

          Message response = connection.request(subject, CacheJsonMapper.mapper.writeValueAsBytes(request), Duration.ofMillis(2000));

          if (response != null) {
            EdgeInitResponse edgeResponse = CacheJsonMapper.mapper.readValue(response.getData(), EdgeInitResponse.class);

            if (Boolean.TRUE.equals(edgeResponse.getSuccess())) {
              inflightSSEListenerRequest.success(edgeResponse);
              return;
            }
          }
        } catch (Exception e) {
          log.error("Failed to communicate with cache", e);
        }

        inflightSSEListenerRequest.reject();
      });
    }
  }

  public EdgeInitPermissionResponse requestPermission(String namedCache, String apiKey, String environmentId, String featureKey) {
    String subject = namedCache + "/" + ChannelConstants.EDGE_CACHE_CHANNEL;
    try {
      Message response = connection.request(subject,
        CacheJsonMapper.mapper.writeValueAsBytes(new EdgeInitRequest().command(EdgeInitRequestCommand.PERMISSION).apiKey(apiKey).environmentId(environmentId).featureKey(featureKey)),
        Duration.ofMillis(2000)
      );

      if (response != null) {
        return CacheJsonMapper.mapper.readValue(response.getData(), EdgeInitPermissionResponse.class);
      }
    } catch (Exception e) {
      log.error("Failed request for cache {}, apiKey {}, envId {}, key {}", namedCache, apiKey, environmentId, featureKey);
    }

    return null;
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
  public void removeInflightSSEListenerRequest(String key) {
    inflightSSEListenerRequests.remove(key);
  }

  protected Environment getEnvironmentFeaturesBySdk(String url, String namedCache, String apiKey,
                                                              String envId) {
    EdgeInitRequest request = new EdgeInitRequest()
      .command(EdgeInitRequestCommand.LISTEN)
      .apiKey(apiKey)
      .environmentId(envId);

    try {
      String subject = namedCache + "/" + ChannelConstants.EDGE_CACHE_CHANNEL;

      Message response = connection.request(subject, CacheJsonMapper.mapper.writeValueAsBytes(request),
        Duration.ofMillis(2000));

      if (response != null) {
        EdgeInitResponse edgeResponse = CacheJsonMapper.mapper.readValue(response.getData(),
          EdgeInitResponse.class);


        return new Environment().id(envId).features(featureTransformer
          .transform(edgeResponse.getFeatures()));

      }
    } catch (Exception e) {
      log.error("Failed to request ");
    }

    return null;
  }

  public List<Environment> requestFeatures(List<String> sdkUrl) {
    List<CompletableFuture<Environment>> futures = new ArrayList<>();

    sdkUrl.forEach(url -> {
      String[] parts = url.split("/");
      if (parts.length == 3) {

        futures.add(CompletableFuture.supplyAsync(() -> getEnvironmentFeaturesBySdk(url, parts[0], parts[2], parts[1]),
          listenExecutor));
      }
    });

    if (!futures.isEmpty()) {
      try {
        return
          CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).thenApply((f) -> futures.stream()
          .map(CompletableFuture::join).collect(Collectors.toList())).get().stream()
            .filter(Objects::nonNull).collect(Collectors.toList());
      } catch (InterruptedException|ExecutionException e) {
        log.error("GET failed for features.", e);
      }
    }

    return new ArrayList<>();
  }
}
