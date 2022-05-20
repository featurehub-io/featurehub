package io.featurehub.web.security.oauth

import cd.connect.app.config.ConfigKey
import cd.connect.app.config.DeclaredConfigResolver
import io.featurehub.web.security.oauth.providers.OAuth2Provider
import jakarta.inject.Inject
import jakarta.ws.rs.client.Client
import jakarta.ws.rs.client.Entity
import jakarta.ws.rs.core.Form
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import org.slf4j.LoggerFactory
import java.util.*

class OAuth2JerseyClient @Inject constructor(protected val client: Client) : OAuth2Client {
  // the url we pass to the POST to confirm we are who we say we are
  @ConfigKey("oauth2.redirectUrl")
  protected var redirectUrl: String? = null
  override fun requestAccess(code: String?, provider: OAuth2Provider): AuthClientResult? {
    val form = Form()
    form.param("grant_type", "authorization_code")
    form.param("client_id", provider.clientId)

    if (!provider.isSecretInHeader()) {
      form.param("client_secret", provider.clientSecret)
    }

    form.param("redirect_uri", redirectUrl)
    form.param("code", code)
    val target = client.target(provider.requestTokenUrl())
    var request = target.request()

    provider.enhanceTokenRequest(request, form)
    val entity = Entity.entity(form, MediaType.APPLICATION_FORM_URLENCODED_TYPE)

    if (provider.isSecretInHeader()) {
      val code = "${provider.clientId}:${provider.clientSecret}"
      request = request.header("Authorization",
        "Basic " + Base64.getEncoder().encodeToString(code.toByteArray()))
    }

    val response = request
      .accept(MediaType.APPLICATION_JSON)
      .post(entity)
    return if (response.statusInfo.family == Response.Status.Family.SUCCESSFUL) {
      response.readEntity(AuthClientResult::class.java)
    } else {
      log.warn("OAuth2 Login attempt failed! {}", response.status)
      null
    }
  }

  companion object {
    private val log = LoggerFactory.getLogger(OAuth2JerseyClient::class.java)
  }

  init {
    DeclaredConfigResolver.resolve(this)
  }
}
