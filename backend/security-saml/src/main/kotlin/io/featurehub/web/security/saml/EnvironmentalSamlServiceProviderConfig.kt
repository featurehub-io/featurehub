package io.featurehub.web.security.saml

import cd.connect.jersey.common.LoggingConfiguration
import com.onelogin.saml2.settings.Saml2Settings
import io.featurehub.jersey.config.CommonConfiguration
import io.featurehub.utils.FallbackPropertyConfig
import io.featurehub.web.security.oauth.AuthProviderInfo
import io.featurehub.web.security.oauth.SSOProviderCollection
import io.featurehub.web.security.oauth.providers.SSOProviderCustomisation
import jakarta.inject.Inject
import jakarta.ws.rs.client.Client
import jakarta.ws.rs.client.ClientBuilder
import jakarta.ws.rs.core.Feature
import jakarta.ws.rs.core.FeatureContext
import org.glassfish.hk2.api.Immediate
import org.glassfish.jersey.internal.inject.AbstractBinder
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.security.PrivateKey
import java.security.cert.X509Certificate

class SamlEnvironmentalFeature : Feature {
  override fun configure(context: FeatureContext): Boolean {
    context.register(SamlResource::class.java)
    context.register(object: AbstractBinder() {
      override fun configure() {
        bind(SamlResponseProviderImpl::class.java)
          .to(SamlImplementationProvider::class.java)

        bind(EnvironmentSamlSourceProviderImpl::class.java)
          .to(EnvironmentSamlSourceProvider::class.java)

        bind(EnvironmentSamlSources::class.java)
          .to(SamlConfigSources::class.java)
          .to(SSOProviderCollection::class.java)
          .`in`(Immediate::class.java)
      }
    })

    return true
  }

}

class SamlConfigAsAuthProvider(private val config: SamlServiceProviderConfig) : AuthProviderInfo {
  override val code: String
    get() = config.samlProviderName

  override val exposeOnLoginPage: Boolean
    get() = (config.icon != null && config.buttonBackgroundColor != null && config.buttonText != null)

  override val icon: SSOProviderCustomisation?
    get() = if (config.icon != null && config.buttonBackgroundColor != null && config.buttonText != null)
        SSOProviderCustomisation(config.icon!!, config.buttonBackgroundColor!!, config.buttonText!!) else null
}

//  these two are primarily for testability
interface EnvironmentSamlSourceProvider {
  fun create(code: String, client: Client): SamlServiceProviderConfig
}

class EnvironmentSamlSourceProviderImpl : EnvironmentSamlSourceProvider {
  override fun create(code: String, client: Client): SamlServiceProviderConfig {
    return EnvironmentalSamlServiceProviderConfig(code, client)
  }

}

class EnvironmentSamlSources @Inject constructor(provider: EnvironmentSamlSourceProvider) : SamlConfigSources, SSOProviderCollection {
  private val log: Logger = LoggerFactory.getLogger(EnvironmentSamlSources::class.java)
  private val idpProviders: Map<String, SamlServiceProviderConfig>
  private val idpProvidersAsAuthProviders: List<AuthProviderInfo>

  init {
    val client = ClientBuilder.newClient()
      .register(CommonConfiguration::class.java)
      .register(LoggingConfiguration::class.java)

    idpProviders =
      FallbackPropertyConfig.getConfig("saml.idp-providers", "")
          .split(",")
          .map { it.trim() }
          .filter { it.isNotBlank() }
          .associateWith { provider.create(it, client) }

    idpProvidersAsAuthProviders = idpProviders.values.map { SamlConfigAsAuthProvider(it) }

    if (idpProviders.isEmpty()) {
      log.info("SAML config: there are no SAML providers configured.")
    } else {
      log.info("SAML sources: {}", idpProviders.keys)
    }
  }

  override fun getSourceFromRegistrationId(registrationId: String): SamlServiceProviderConfig? {
    return idpProviders[registrationId]
  }

  override val providers: Collection<AuthProviderInfo>
    get() = idpProvidersAsAuthProviders

  override fun requestRedirectUrl(provider: String): String? {
    val found = idpProviders[provider]
    return if (found != null) SamlRequestGenerator(found).createRequest() else null
  }
}

class EnvironmentalSamlServiceProviderConfig(private val samlProvider: String, client: Client) : SamlServiceProviderConfig {
  private var _idpMetadataText: String
  private val _entityIdUri: String
  private val _spBaseUrl: String
  private val _signatureVerifier: SamlCertificateConverter
  private val _saml2Settings: FeatureHubSaml2Settings
  private val _iconUrl: String?
  private val _buttonBackgroundColor: String?
  private val _buttonText: String?
  private val _emailDomainMatching: List<String>

  init {
    _entityIdUri = FallbackPropertyConfig.getMandatoryConfig("saml.${samlProvider}.idp.entity-id")
    _spBaseUrl = FallbackPropertyConfig.getMandatoryConfig("saml.${samlProvider}.sp.base-url")

    _iconUrl = FallbackPropertyConfig.getConfig("saml.${samlProvider}.login.icon-url")
    _buttonBackgroundColor = FallbackPropertyConfig.getConfig("saml.${samlProvider}.login.button-background-color")
    _buttonText = FallbackPropertyConfig.getConfig("saml.${samlProvider}.login.button-text")

    _emailDomainMatching = FallbackPropertyConfig.getConfig("saml.${samlProvider}.email-domains", "")
      .split(",").map { it.trim() }.filter { it.isNotEmpty() }.map { it.lowercase() }

    _signatureVerifier = SamlCertificateConverter(
      FallbackPropertyConfig.getMandatoryConfig("saml.${samlProvider}.sp.x509-cert"),
      FallbackPropertyConfig.getConfig("saml.${samlProvider}.sp.x509-cert-new"),
      FallbackPropertyConfig.getMandatoryConfig("saml.${samlProvider}.sp.private-key"),
      FallbackPropertyConfig.getConfig("saml.${samlProvider}.sp.private-key-alg", "RSA")
    )

    val idpMetadataPath = FallbackPropertyConfig.getConfig("saml.${samlProvider}.idp.metadata-file")
    var metadataText: String? = null

    if (idpMetadataPath != null) {
      metadataText = File(idpMetadataPath).readText()
    }

    if (metadataText == null) {
      // they could load it using a tool into an environment variable for instance - such as Pulumi
      metadataText = FallbackPropertyConfig.getConfig("saml.${samlProvider}.idp.metadata-text")
    }

    // ok, its neither of these, so load it from the remote source
    if (metadataText == null) {
      // now go and get the metadata for this host
      val response = client.target(_entityIdUri).request().get()
      metadataText = response.readEntity(String::class.java)
    }

    _idpMetadataText = metadataText!!

    _saml2Settings = FeatureHubSaml2Settings(this)
  }

  override val samlProviderName: String
    get() = samlProvider

  override val idpEntityId: String
    get() = _entityIdUri
  override val spBaseUrl: String
    get() = _spBaseUrl
  override val idpMetadataText: String
    get() = _idpMetadataText
  override val spX509Cert: X509Certificate
    get() = _signatureVerifier.x509Certificate
  override val spX509CertNew: X509Certificate?
    get() = _signatureVerifier.x509CertificateNew
  override val spPrivateKey: PrivateKey
    get() = _signatureVerifier.privateKey
  override val icon: String?
    get() = _iconUrl
  override val buttonBackgroundColor: String?
    get() = _buttonBackgroundColor
  override val buttonText: String?
    get() = _buttonText
  override val mustMatchEmailDomains: List<String>
    get() = _emailDomainMatching
  override val debug: Boolean
    get() = FallbackPropertyConfig.getConfig("saml.${samlProvider}.debug", "false") == "true"

  override fun saml2Settings(): Saml2Settings {
    return _saml2Settings
  }
}
