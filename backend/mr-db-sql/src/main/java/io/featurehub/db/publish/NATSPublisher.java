package io.featurehub.db.publish;

import cd.connect.app.config.ConfigKey;
import cd.connect.app.config.DeclaredConfigResolver;
import cd.connect.lifecycle.ApplicationLifecycleManager;
import cd.connect.lifecycle.LifecycleStatus;
import io.ebean.Database;
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
  private String id;
  private Map<String, NamedCacheListener> namedCaches = new ConcurrentHashMap<>();

  @Inject
  public NATSPublisher(Database database, CacheSource cacheSource) {
    this.database = database;
    this.cacheSource = cacheSource;
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
    }

    new QDbNamedCache().findList().forEach(nc -> {
      namedCaches.put(nc.getCacheName(), new NamedCacheListener(nc.getCacheName(), connection, id, this.cacheSource));
    });

    ApplicationLifecycleManager.registerListener(trans -> {
      if (trans.next == LifecycleStatus.TERMINATING) {
        shutdown();
      }
    });
  }

  private void shutdown() {
    namedCaches.values().parallelStream().forEach(NamedCacheListener::close);
  }
}
