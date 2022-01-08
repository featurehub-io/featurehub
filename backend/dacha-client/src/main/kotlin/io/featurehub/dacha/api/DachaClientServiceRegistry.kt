package io.featurehub.dacha.api

interface DachaClientServiceRegistry {
    fun getEnvironmentService(cache: String): DachaEnvironmentService?
    fun getApiKeyService(cache: String): DachaApiKeyService

    /**
     * This lets us register a internal implementation which just calls the required code if the Dacha and Edge
     * are wired together as one service, as they are in the Party Service.
     *
     * @param cache
     * @param apiKeyService
     */
    fun registerApiKeyService(cache: String, apiKeyService: DachaApiKeyService)
}
