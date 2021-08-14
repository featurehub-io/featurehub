package io.featurehub.dacha.api;

public interface DachaClientServiceRegistry {
  DachaEnvironmentService getEnvironmentService(String cache);
  DachaApiKeyService getApiKeyService(String cache);
}
