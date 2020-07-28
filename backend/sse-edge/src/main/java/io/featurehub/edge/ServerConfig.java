package io.featurehub.edge;

import cd.connect.app.config.ConfigKey;
import cd.connect.app.config.DeclaredConfigResolver;
import cd.connect.lifecycle.ApplicationLifecycleManager;
import cd.connect.lifecycle.LifecycleStatus;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.featurehub.dacha.api.CacheJsonMapper;
import io.featurehub.edge.client.TimedBucketClientConnection;
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
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
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

public class ServerConfig {
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
  private Map<String, List<TimedBucketClientConnection>> clientBuckets = new ConcurrentHashMap<>();
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

      final List<TimedBucketClientConnection> tbc = clientBuckets.get(fv.getEnvironmentId());
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

  public void requestFeatures(final TimedBucketClientConnection client) {
    clientBuckets.computeIfAbsent(client.getEnvironmentId(), (k) -> new ArrayList<>()).add(client);

    client.registerEjection(this::clientRemoved);

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
  private void clientRemoved(TimedBucketClientConnection client) {
    final List<TimedBucketClientConnection> conns = clientBuckets.get(client.getEnvironmentId());

    if (conns != null) {
      conns.remove(client);
    }
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


        return new Environment().sdkUrl(url).features(featureTransformer
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
