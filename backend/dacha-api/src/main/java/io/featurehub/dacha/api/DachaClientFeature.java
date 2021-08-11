package io.featurehub.dacha.api;

import cd.connect.app.config.ConfigKey;
import cd.connect.app.config.DeclaredConfigResolver;
import cd.connect.jersey.common.LoggingConfiguration;
import cd.connect.openapi.support.ApiClient;
import io.featurehub.dacha.api.impl.DachaApiKeyServiceServiceImpl;
import io.featurehub.dacha.api.impl.DachaEnvironmentServiceServiceImpl;
import io.featurehub.jersey.config.CommonConfiguration;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.core.Feature;
import jakarta.ws.rs.core.FeatureContext;
import org.glassfish.jersey.internal.inject.AbstractBinder;

public class DachaClientFeature implements Feature  {
  @ConfigKey("dacha.url")
  String dachaUrl = "http://localhost:8094";

  public DachaClientFeature() {
    DeclaredConfigResolver.resolve(this);
  }

  @Override
  public boolean configure(FeatureContext featureContext) {
    final Client client =
      ClientBuilder.newClient().register(CommonConfiguration.class).register(LoggingConfiguration.class);

    final ApiClient apiClient = new ApiClient(client, dachaUrl);

    featureContext.register(new AbstractBinder() {
      @Override
      protected void configure() {
        bind(new DachaEnvironmentServiceServiceImpl(apiClient)).to(DachaEnvironmentService.class);
        bind(new DachaApiKeyServiceServiceImpl((apiClient))).to(DachaApiKeyService.class);
      }
    });

    return true;
  }
}
