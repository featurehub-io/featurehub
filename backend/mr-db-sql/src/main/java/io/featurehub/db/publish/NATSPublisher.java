package io.featurehub.db.publish;

import cd.connect.app.config.ConfigKey;
import cd.connect.app.config.DeclaredConfigResolver;
import cd.connect.lifecycle.ApplicationLifecycleManager;
import cd.connect.lifecycle.LifecycleStatus;
import io.featurehub.db.listener.EdgeUpdateListener;
import io.featurehub.db.listener.FeatureUpdateBySDKApi;
import io.featurehub.db.listener.FeatureUpdateListener;
import io.featurehub.db.model.query.QDbNamedCache;
import io.featurehub.publish.ChannelConstants;
import io.featurehub.publish.NATSSource;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 */

@Singleton
public class NATSPublisher implements PublishManager {
  private final CacheSource cacheSource;
  private final UUID id;
  private final Map<String, NamedCacheListener> namedCaches = new ConcurrentHashMap<>();
  private final Map<String, EdgeUpdateListener> edgeFeatureUpdateListeners = new ConcurrentHashMap<>();
  @ConfigKey("feature-update.listener.enable")
  public Boolean enableListener = Boolean.TRUE;

  @Inject
  public NATSPublisher(CacheSource cacheSource, FeatureUpdateBySDKApi featureUpdateBySDKApi,
                       NATSSource natsServer, EdgeUpdateListenerSource edgeUpdateListenerSource) {
    this.cacheSource = cacheSource;

    DeclaredConfigResolver.resolve(this);

    id = UUID.randomUUID();

    // always listen to default
    if (new QDbNamedCache().findCount() == 0) {
      namedCaches.put(ChannelConstants.DEFAULT_CACHE_NAME, new NamedCacheListener(ChannelConstants.DEFAULT_CACHE_NAME
        , natsServer.getConnection(), id, cacheSource));
      if (enableListener) {
        edgeFeatureUpdateListeners.put(ChannelConstants.DEFAULT_CACHE_NAME,
          edgeUpdateListenerSource.createListener(ChannelConstants.DEFAULT_CACHE_NAME, natsServer.getConnection(),
          featureUpdateBySDKApi));
      }
    }

    new QDbNamedCache().findList().forEach(nc -> {
      namedCaches.put(nc.getCacheName(), new NamedCacheListener(nc.getCacheName(), natsServer.getConnection(), id, this.cacheSource));
      edgeFeatureUpdateListeners.put(nc.getCacheName(),
        edgeUpdateListenerSource.createListener(nc.getCacheName(), natsServer.getConnection(), featureUpdateBySDKApi));
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
