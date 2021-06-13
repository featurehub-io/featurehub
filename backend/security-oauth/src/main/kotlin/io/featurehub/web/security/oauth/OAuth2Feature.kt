package io.featurehub.web.security.oauth;

import cd.connect.app.config.ConfigKey;
import cd.connect.app.config.DeclaredConfigResolver;
import cd.connect.jersey.common.CommonConfiguration;
import cd.connect.jersey.common.LoggingConfiguration;
import io.featurehub.web.security.oauth.providers.AzureProvider;
import io.featurehub.web.security.oauth.providers.GithubProvider;
import io.featurehub.web.security.oauth.providers.GoogleProvider;
import io.featurehub.web.security.oauth.providers.OAuth2Provider;
import io.featurehub.web.security.oauth.providers.OAuth2ProviderDiscovery;
import io.featurehub.web.security.oauth.providers.OAuth2ProviderManager;
import org.glassfish.jersey.internal.inject.AbstractBinder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Feature;
import javax.ws.rs.core.FeatureContext;
import java.util.ArrayList;
import java.util.List;

public class OAuth2Feature implements Feature {
  private static final Logger log = LoggerFactory.getLogger(OAuth2Feature.class);
  // a comma separated list of valid providers
  @ConfigKey("oauth2.providers")
  protected List<String> validProviderSources = new ArrayList<>();

  public OAuth2Feature() {
    DeclaredConfigResolver.resolve(this);
  }

  @Override
  public boolean configure(FeatureContext context) {
    if (!validProviderSources.isEmpty()) {
      List<Class<? extends OAuth2Provider>> providers = new ArrayList<>();
      if (validProviderSources.contains(GoogleProvider.PROVIDER_NAME)) {
        providers.add(GoogleProvider.class);
      }
      if (validProviderSources.contains(AzureProvider.PROVIDER_NAME)) {
        providers.add(AzureProvider.class);
      }
      if (validProviderSources.contains(GithubProvider.Companion.getPROVIDER_NAME())) {
        providers.add(GithubProvider.class);
      }
      if (providers.isEmpty()) {
        throw new RuntimeException("oauth2.providers list is not empty and contains unsupported oauth2 providers.");
      }
      context.register(OauthResource.class);
      context.register(new AbstractBinder() {
        @Override
        protected void configure() {
          // bind all the providers
          providers.forEach(p -> bind(p).to(OAuth2Provider.class).in(Singleton.class));

          // the class that allows discovery of the providers
          bind(OAuth2ProviderManager.class).to(OAuth2ProviderDiscovery.class).to(AuthProvider.class).in(Singleton.class);

          bind(buildClient()).to(Client.class).in(Singleton.class);

          // now the outbound http request to validte authorization flow
          bind(OAuth2JerseyClient.class).to(OAuth2Client.class).in(Singleton.class);
        }
      });
    } else {
      log.info("No oauth2 providers in config, skipping oauth2.");
    }

    return true;
  }

  protected Client buildClient() {
    return ClientBuilder.newClient()
      .register(CommonConfiguration.class)
      .register(LoggingConfiguration.class);
  }
}
