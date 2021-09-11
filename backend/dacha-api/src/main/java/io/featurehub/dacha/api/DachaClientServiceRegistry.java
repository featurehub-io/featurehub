package io.featurehub.dacha.api;

public interface DachaClientServiceRegistry {
  DachaEnvironmentService getEnvironmentService(String cache);
  DachaApiKeyService getApiKeyService(String cache);

  /**
   * This lets us register a internal implementation which just calls the required code if the Dacha and Edge
   * are wired together as one service, as they are in the Party Service.
   *
   * @param cache
   * @param apiKeyService
   */
  void registerApiKeyService(String cache, DachaApiKeyService apiKeyService);
}
