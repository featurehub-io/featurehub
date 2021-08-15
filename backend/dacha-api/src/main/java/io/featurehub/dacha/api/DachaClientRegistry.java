package io.featurehub.dacha.api;

import cd.connect.app.config.ConfigKey;
import cd.connect.app.config.DeclaredConfigResolver;
import cd.connect.jersey.common.LoggingConfiguration;
import cd.connect.openapi.support.ApiClient;
import io.featurehub.dacha.api.impl.DachaApiKeyServiceServiceImpl;
import io.featurehub.dacha.api.impl.DachaEnvironmentServiceServiceImpl;
import io.featurehub.jersey.config.CommonConfiguration;
import io.prometheus.client.Counter;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import org.glassfish.jersey.client.ClientProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DachaClientRegistry implements DachaClientServiceRegistry {
  private static final Logger log = LoggerFactory.getLogger(DachaClientRegistry.class);
  private final Client client;
  private final Map<String, DachaEnvironmentService> environmentServiceMap = new ConcurrentHashMap<>();
  private final Map<String, DachaApiKeyService>  apiServiceMap = new ConcurrentHashMap<>();
  private final Counter cacheHitCounter = new Counter.Builder().name("dacha_client_cache_hit")
    .help("Number of times cache was hit when requesting dacha client").register();
  private final Counter cacheMissCounter = new Counter.Builder().name("dacha_client_cache_miss")
    .help("Number of times the cache was missed when requesting dacha client").register();

  @ConfigKey("dacha.timeout.connect")
  Integer connectTimeout = 4000;
  @ConfigKey("dacha.timeout.read")
  Integer readTimeout = 4000;

  public DachaClientRegistry() {
    DeclaredConfigResolver.resolve(this);

    client =
        ClientBuilder.newClient()
            .register(CommonConfiguration.class)
            .register(LoggingConfiguration.class);

    client.property(ClientProperties.CONNECT_TIMEOUT, connectTimeout);
    client.property(ClientProperties.READ_TIMEOUT,    readTimeout);
  }

  private String url(String cache) {
    return System.getProperty("dacha.url." + cache, System.getenv("DACHA.URL." + cache.toUpperCase()));
  }

  public DachaApiKeyService getApiKeyService(String cache) {
    cache = cache.toLowerCase();

    DachaApiKeyService service = apiServiceMap.get(cache);

    if (service == null) {
      String url = url(cache);

      if (url != null) {
        service = new DachaApiKeyServiceServiceImpl(new ApiClient(client, url));
        apiServiceMap.put(cache, service);
      } else {
        log.warn("Request for missing cache {}", cache);
      }
    }

    if (service == null) {
      cacheMissCounter.inc();
    } else {
      cacheHitCounter.inc();
    }

    return service;
  }

  @Override
  public DachaEnvironmentService getEnvironmentService(String cache) {
    cache = cache.toLowerCase();

    DachaEnvironmentService service = environmentServiceMap.get(cache);

    if (service == null) {
      String url = url(cache);

      if (url != null) {
        service = new DachaEnvironmentServiceServiceImpl(new ApiClient(client, url));

        environmentServiceMap.put(cache, service);
      }
    }

    if (service == null) {
      cacheMissCounter.inc();
    } else {
      cacheHitCounter.inc();
    }

    return service;
  }
}
