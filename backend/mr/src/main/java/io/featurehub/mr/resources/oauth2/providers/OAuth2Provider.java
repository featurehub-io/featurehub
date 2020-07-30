package io.featurehub.mr.resources.oauth2.providers;

import io.featurehub.mr.resources.oauth2.AuthClientResult;

public interface OAuth2Provider {
  ProviderUser discoverProviderUser(AuthClientResult authed);

  String providerName();

  String requestTokenUrl();
  String requestAuthorizationUrl();

  String getClientId();

  String getClientSecret();
}
