package io.featurehub.web.security.oauth.providers

import io.featurehub.web.security.oauth.AuthProviderInfo
import jakarta.inject.Inject
import org.glassfish.hk2.api.IterableProvider
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.*
import java.util.function.Consumer

data class OAuth2ProviderInfo(
  override val code: String,
  override val exposeOnLoginPage: Boolean,
  override val icon: SSOProviderCustomisation?) : AuthProviderInfo

class OAuth2ProviderManager @Inject constructor(oAuth2Providers: IterableProvider<OAuth2Provider>) :
  OAuth2ProviderDiscovery {
  protected var providerMap: MutableMap<String, OAuth2Provider> = HashMap()
  override fun getProvider(id: String?): OAuth2Provider? {
    return providerMap[id]
  }

  override fun getProviderFromState(state: String): OAuth2Provider? {
    val semi = state.indexOf(";")
    return if (semi > 0) {
      getProvider(state.substring(0, semi))
    } else null
  }

  override val providers: Collection<AuthProviderInfo>
    get() = providerMap.map { p -> OAuth2ProviderInfo(p.key, true, p.value.providerIcon()) }

  override fun requestRedirectUrl(provider: String): String {
    // TODO: store state to ensure valid callback and XSRF attacks
    val state = URLEncoder.encode(provider + ";" + UUID.randomUUID().toString(), StandardCharsets.UTF_8)
    return providerMap[provider]!!.requestAuthorizationUrl() + "&state=" + state
  }

  init {
    oAuth2Providers.forEach(Consumer { p: OAuth2Provider -> providerMap[p.providerName()] = p })
  }
}
