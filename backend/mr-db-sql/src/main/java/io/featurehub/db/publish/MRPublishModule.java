package io.featurehub.db.publish;

import cd.connect.app.config.ConfigKey;
import cd.connect.app.config.DeclaredConfigResolver;
import jakarta.inject.Singleton;
import org.glassfish.jersey.internal.inject.AbstractBinder;

public class MRPublishModule  extends AbstractBinder {
  @ConfigKey("nats.urls")
  String natsServer = "";

  public MRPublishModule() {
    DeclaredConfigResolver.resolve(this);
  }

  @Override
  protected void configure() {
    if (natsServer.length() == 0) {
      bind(DummyPublisher.class).to(PublishManager.class).to(CacheSource.class).in(Singleton.class);
    } else {
      bind(NATSPublisher.class).to(PublishManager.class).in(Singleton.class);
      bind(DbCacheSource.class).to(CacheSource.class).in(Singleton.class);
    }
  }
}
