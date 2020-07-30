package io.featurehub.mr.resources.oauth2.providers;

import io.featurehub.mr.resources.auth.AuthProvider;

public interface OAuth2ProviderDiscovery extends AuthProvider {
  OAuth2Provider getProvider(String id);

  OAuth2Provider getProviderFromState(String state);
}
