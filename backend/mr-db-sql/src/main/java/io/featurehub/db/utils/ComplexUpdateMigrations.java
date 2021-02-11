package io.featurehub.db.utils;

import io.featurehub.db.api.ServiceAccountApi;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.jersey.server.spi.Container;
import org.glassfish.jersey.server.spi.ContainerLifecycleListener;

public class ComplexUpdateMigrations implements ContainerLifecycleListener {
  @Override
  public void onStartup(Container container) {
    ServiceLocator injector = container.getApplicationHandler()
      .getInjectionManager().getInstance(ServiceLocator.class);

    final ServiceAccountApi serviceAccountApi = injector.getService(ServiceAccountApi.class);
    serviceAccountApi.cleanupServiceAccountApiKeys();
  }

  @Override
  public void onReload(Container container) {

  }

  @Override
  public void onShutdown(Container container) {

  }
}
