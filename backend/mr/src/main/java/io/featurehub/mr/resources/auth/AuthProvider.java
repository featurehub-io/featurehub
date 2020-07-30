package io.featurehub.mr.resources.auth;

import java.util.Collection;

public interface AuthProvider {
  Collection<String> getProviders();

  String requestRedirectUrl(String provider);
}
