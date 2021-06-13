package io.featurehub.web.security.oauth;

import java.util.Collection;

public interface AuthProvider {
  Collection<String> getProviders();

  String requestRedirectUrl(String provider);
}
