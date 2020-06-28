package io.featurehub.db.publish;

import cd.connect.app.config.ConfigKey;
import cd.connect.app.config.DeclaredConfigResolver;
import cd.connect.lifecycle.ApplicationLifecycleManager;
import cd.connect.lifecycle.LifecycleStatus;
import io.ebean.Database;
import io.featurehub.db.listener.EdgeUpdateListener;
import io.featurehub.db.listener.FeatureUpdateBySDKApi;
import io.featurehub.db.listener.FeatureUpdateListener;
import io.featurehub.db.model.query.QDbNamedCache;
import io.featurehub.publish.ChannelConstants;
import io.nats.client.Connection;
import io.nats.client.Nats;
import io.nats.client.Options;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 */

@Singleton
public class NATSPublisher implements PublishManager {
  @ConfigKey("nats.urls")
  public String natsServer;
  private Connection connection;
  private final Database database;
  private final CacheSource cacheSource;
  private final FeatureUpdateBySDKApi featureUpdateBySDKApi;
  private String id;
  private Map<String, NamedCacheListener> namedCaches = new ConcurrentHashMap<>();
  private Map<String, EdgeUpdateListener> edgeFeatureUpdateListeners = new ConcurrentHashMap<>();
  @ConfigKey("feature-update.listener.enable")
  public Boolean enableListener = Boolean.TRUE;

  @Inject
  public NATSPublisher(Database database, CacheSource cacheSource, FeatureUpdateBySDKApi featureUpdateBySDKApi) {
    this.database = database;
    this.cacheSource = cacheSource;
    this.featureUpdateBySDKApi = featureUpdateBySDKApi;
    DeclaredConfigResolver.resolve(this);

    Options options = new Options.Builder().server(natsServer).build();
    try {
      connection = Nats.connect(options);
    } catch (IOException |InterruptedException e) {
      // should fail if we can't connect
      throw new RuntimeException(e);
    }

    id = UUID.randomUUID().toString();

    // always listen to default
    if (new QDbNamedCache().findCount() == 0) {
      namedCaches.put(ChannelConstants.DEFAULT_CACHE_NAME, new NamedCacheListener(ChannelConstants.DEFAULT_CACHE_NAME, connection, id, cacheSource));
      if (enableListener) {
        edgeFeatureUpdateListeners.put(ChannelConstants.DEFAULT_CACHE_NAME, new FeatureUpdateListener(ChannelConstants.DEFAULT_CACHE_NAME, connection, featureUpdateBySDKApi));
      }
    }

    new QDbNamedCache().findList().forEach(nc -> {
      namedCaches.put(nc.getCacheName(), new NamedCacheListener(nc.getCacheName(), connection, id, this.cacheSource));
      edgeFeatureUpdateListeners.put(nc.getCacheName(), new FeatureUpdateListener(nc.getCacheName(), connection, featureUpdateBySDKApi));
    });

    ApplicationLifecycleManager.registerListener(trans -> {
      if (trans.next == LifecycleStatus.TERMINATING) {
        shutdown();
      }
    });
  }

  private void shutdown() {
    namedCaches.values().parallelStream().forEach(NamedCacheListener::close);
    edgeFeatureUpdateListeners.values().parallelStream().forEach(EdgeUpdateListener::close);
  }
}
