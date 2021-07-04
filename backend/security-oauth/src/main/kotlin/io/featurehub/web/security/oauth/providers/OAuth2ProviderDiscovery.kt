package io.featurehub.web.security.oauth.providers

import io.featurehub.web.security.oauth.AuthProvider

interface OAuth2ProviderDiscovery : AuthProvider {
    fun getProvider(id: String?): OAuth2Provider?
    fun getProviderFromState(state: String): OAuth2Provider?
}
