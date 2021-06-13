package io.featurehub.web.security.oauth.providers;

import io.featurehub.web.security.oauth.AuthClientResult;

public interface OAuth2Provider {
  ProviderUser discoverProviderUser(AuthClientResult authed);

  String providerName();

  String requestTokenUrl();
  String requestAuthorizationUrl();

  String getClientId();

  String getClientSecret();
}
