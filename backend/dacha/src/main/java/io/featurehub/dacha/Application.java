package io.featurehub.dacha;

import cd.connect.lifecycle.ApplicationLifecycleManager;
import cd.connect.lifecycle.LifecycleStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Application {
  private static final Logger log = LoggerFactory.getLogger(Application.class);

  /**
   * initialises and starts the dacha cache layer.
   */
  public static void init() {
    final InMemoryCache inMemoryCache = new InMemoryCache();
    final ServerConfig serverConfig = new ServerConfig(inMemoryCache);
    CacheManager cm = new CacheManager(inMemoryCache, serverConfig);
    cm.init();
  }

  public static void main(String[] args) {
    try {
      init();
      ApplicationLifecycleManager.updateStatus(LifecycleStatus.STARTED);

      log.info("Cache has started");

      Thread.currentThread().join();
    } catch (Exception e) {
      log.error("Failed to start");
    }
  }
}
