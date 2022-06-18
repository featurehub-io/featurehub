package io.featurehub.web.security.oauth.providers

import io.featurehub.web.security.oauth.SSOProviderCollection

interface OAuth2ProviderDiscovery : SSOProviderCollection {
  fun getProvider(id: String?): OAuth2Provider?
  fun getProviderFromState(state: String): OAuth2Provider?
}
