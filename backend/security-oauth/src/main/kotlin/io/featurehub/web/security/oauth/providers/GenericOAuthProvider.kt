package io.featurehub.web.security.oauth.providers

import cd.connect.app.config.ConfigKey
import cd.connect.app.config.DeclaredConfigResolver
import io.featurehub.web.security.oauth.AuthClientResult
import jakarta.ws.rs.client.Invocation
import jakarta.ws.rs.core.Form
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/**
 * A generic OAuth2 provider. This lets us specify as much as possible about the provider, including the
 * way it will appear on the front end. This may get more detailed over time depending on requirements.
 *
 * Most of the other providers are convenience layers on top of this one, they know what the URL is, etc, but
 * in some cases (e.g. Github) they require some extra tweaking. This one bases itself on the OAuth provider returning
 * a JWT and the email and name being available in that.
 *
 * How to test:
 * Set yourself up for Keycloak according to your documentation. You will also need to serve an icon that is a PNG or JPEG
 * that is 48x48 pixels.
 *
 * If you just want to see the image appearing and make sure it clicks through to some random location, you can do something like:
 *
oauth2.providers.generic.secret=123
oauth2.providers.generic.auth-url=http://localhost:8900
oauth2.providers.generic.id=356
oauth2.providers.generic.scope=profile email
oauth2.providers.generic.token-url=http://localhost:8900/token
oauth2.providers.generic.icon.url=http://localhost:8097/my-icon.jpeg
oauth2.providers.generic.icon.background-color=0xFFF44336
oauth2.providers.generic.icon.text=Sign in with MyCompany
 *
 * Serving an image from your local machine:
 * - download an image to some location on your disk
 * - run nginx in Docker, e.g. : "docker run -p 8097:80 -v $HOME/Downloads:/usr/share/nginx/html nginx" runs docker and points
 * nginx's home directory to your local Downloads folder.
 * - find the docker instance (docker ps), and then exec into it (docker exec -it <id> /bin/bash)
 * - apt-get update && apt-get install vim (or nano, or your editor of choice)
 * - edit the /etc/nginx/conf.d/default.conf file, and where it specifies "location /" make it look like this (you need CORS support):
location / {
  add_header Access-Control-Allow-Origin *;
  root   /usr/share/nginx/html;
  index  index.html index.htm;
}
 * you should now be able to specify your icon as http://localhost:8097/<image-name> in the config.
 */

class GenericOAuthProvider : OAuth2Provider {
  private val log: Logger = LoggerFactory.getLogger(GenericOAuthProvider::class.java)
  @ConfigKey("oauth2.providers.generic.secret")
  override var clientSecret: String? = null
    protected set

  @ConfigKey("oauth2.providers.generic.auth-url")
  var authUrl: String? = null

  @ConfigKey("oauth2.providers.generic.id")
  override var clientId: String? = null
    protected set

  @ConfigKey("oauth2.providers.generic.secret-in-header")
  var secretInHeader: Boolean? = false

  @ConfigKey("oauth2.providers.generic.scope")
  var scope: String? = "profile email"

  @ConfigKey("oauth2.providers.generic.name-fields")
  var nameFields: List<String> = listOf("name")

  @ConfigKey("oauth2.providers.generic.token-url")
  var tokenUrl: String? = null

  @ConfigKey("oauth2.providers.generic.email-field")
  var email: String? = "email"

  @ConfigKey("oauth2.providers.generic.icon.url")
  var iconUrl: String? = null

  @ConfigKey("oauth2.providers.generic.icon.background-color")
  var iconBackgroundColor: String? = null

  @ConfigKey("oauth2.providers.generic.icon.text")
  var iconText: String? = null

  @ConfigKey("oauth2.redirectUrl")
  protected var redirectUrl: String? = null

  @ConfigKey("oauth2.providers.generic.token-header-pairs")
  protected var tokenExtraHeaders: Map<String, String?>? = mapOf()

  @ConfigKey("oauth2.providers.generic.token-form-pairs")
  protected var tokenFormExtraValues: Map<String, String?>? = mapOf()

  @ConfigKey("oauth2.providers.generic.use-access-token")
  protected var useAccessToken: Boolean? = false

  private val actualAuthUrl: String

  init {
    DeclaredConfigResolver.resolve(this)

    val encodedScope = URLEncoder.encode(scope, StandardCharsets.UTF_8)
    val encodedClientId = URLEncoder.encode(clientId, StandardCharsets.UTF_8)
    val encodedUrl = URLEncoder.encode(redirectUrl, StandardCharsets.UTF_8)

    val qAuth = if (authUrl!!.contains("?")) "&" else "?"

    actualAuthUrl =
      "$authUrl${qAuth}client_id=${encodedClientId}&response_type=code&scope=${encodedScope}&redirect_uri=${encodedUrl}"
  }

  override fun discoverProviderUser(authed: AuthClientResult): ProviderUser? {
    val jwt = if (useAccessToken == true) authed.accessToken else authed.idToken
    val idInfo = Jwt.decodeJwt(jwt) ?: return null
    if (idInfo[email] == null) {
      log.error("Unable to discover `email` address in Payload {}", idInfo)
      return null
    }

    val name = StringBuilder()

    nameFields.forEach { field ->
      val value = idInfo[field]?.toString()
      if (value != null) {
        name.append("${value} ")
      }
    }

    val realName = name.toString().trim()
    if (realName.isEmpty()) {
      log.error("Unable to discover the name from the payload {}", idInfo)
      return null
    }

    return ProviderUser.Builder().email(idInfo[email]?.toString())
      .name(realName).build()
  }

  override fun providerName(): String {
    return PROVIDER_NAME
  }

  override fun requestTokenUrl(): String {
    return tokenUrl!!
  }

  override fun requestAuthorizationUrl(): String {
    return actualAuthUrl
  }

  override fun enhanceTokenRequest(request: Invocation.Builder, form: Form) {
    loadExtraHeaders(request, tokenExtraHeaders)
    if (tokenFormExtraValues?.isNotEmpty() == true) {
      tokenFormExtraValues!!.forEach { k, v ->
        if (v != null) {
          form.param(k, v)
        }
      }
    }
  }

  fun loadExtraHeaders(request: Invocation.Builder, headers: Map<String, String?>?) {
    if (headers?.isNotEmpty() == true) {
      headers.forEach { k, v ->
        if (v != null) {
          request.header(k, v)
        }
      }
    }
  }

  override fun providerIcon(): OAuth2ProviderCustomisation {
    return OAuth2ProviderCustomisation(iconUrl!!, iconBackgroundColor!!, iconText!!)
  }

  override fun isSecretInHeader(): Boolean {
    return secretInHeader!!
  }

  companion object {
    const val PROVIDER_NAME = "oauth2-generic"
  }
}
