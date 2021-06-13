package io.featurehub.web.security.oauth.providers;

import io.featurehub.web.security.oauth.AuthProvider;

public interface OAuth2ProviderDiscovery extends AuthProvider {
  OAuth2Provider getProvider(String id);

  OAuth2Provider getProviderFromState(String state);
}
