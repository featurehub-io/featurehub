package io.featurehub.web.security.saml

import cd.connect.app.config.ConfigKey
import cd.connect.app.config.DeclaredConfigResolver
import cd.connect.jersey.prometheus.Prometheus
import com.onelogin.saml2.authn.SamlResponse
import com.onelogin.saml2.settings.Saml2Settings
import io.featurehub.utils.FeatureHubConfig
import io.featurehub.web.security.oauth.SSOCompletionListener
import jakarta.inject.Inject
import jakarta.ws.rs.*
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.URI

interface FeatureHubSamlResponse {
  fun isValid(): Boolean
  fun getNameId(): String

  fun getAttributes(): Map<String, List<String>>

  fun getValidationException(): java.lang.Exception
}

class FeatureHubSamlResponseImpl(settings: Saml2Settings,
                                 samlResponse: String) : SamlResponse(settings,
  settings.spAssertionConsumerServiceUrl.toExternalForm(), samlResponse), FeatureHubSamlResponse

interface SamlImplementationProvider {
  fun decodeResponse(config: SamlServiceProviderConfig, payload: String): FeatureHubSamlResponse
  fun createRequest(config: SamlServiceProviderConfig): String
}

class SamlResponseProviderImpl : SamlImplementationProvider {
  override fun decodeResponse(config: SamlServiceProviderConfig, payload: String): FeatureHubSamlResponse {
    val settings = config.saml2Settings()

    return FeatureHubSamlResponseImpl(settings, payload)
  }

  override fun createRequest(config: SamlServiceProviderConfig): String {
    return SamlRequestGenerator(config).createRequest()
  }
}

@Path("/saml")
class SamlResource @Inject constructor(
  private val samlSources: SamlConfigSources,
  private val completionListener: SSOCompletionListener,
  private val samlImplProvider: SamlImplementationProvider,
  @FeatureHubConfig("saml.adminUiUrlSuccess", required = true) private val samlSuccessUrl: String,
  @FeatureHubConfig("saml.adminUiUrlFailure", required = true) private val samlFailureUrl: String
) {
  private val log: Logger = LoggerFactory.getLogger(SamlResource::class.java)

  @ConfigKey("saml.misconfigured-url")
  var samlMisconfiguredUrl: String? = "https://docs.featurehub.io/featurehub/latest/identity.html#_saml"

  init {
    DeclaredConfigResolver.resolve(this)
  }

  @POST
  @Path("/{registrationId}/sso")
  @Prometheus(name = "saml_sso", help = "SAML SSO Endpoint")
  @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
  fun receiveSamlPayload(
    @FormParam("SAMLResponse") samlPayload: String?,
    @PathParam("registrationId") registrationId: String
  ) : Response {
    if (samlPayload.isNullOrBlank()) {
      throw NotAuthorizedException("expecting SAMLResponse in form and did not receive one")
    }

    val samlSource =
      samlSources.getSourceFromRegistrationId(registrationId) ?: throw NotAuthorizedException("No such registration id")

    // if we have no organisation, we need to insist they go back to the start
    if (!completionListener.initialAppSetupComplete()) {
      return Response.status(302).location(URI.create(samlFailureUrl)).build()
    }

    try {
      val samlResponse = samlImplProvider.decodeResponse(samlSource, samlPayload)
      if (samlResponse.isValid()) {
        val email = samlResponse.getNameId()

        if (!matchEmail(email, samlSource.mustMatchEmailDomains)) {
          log.warn("Invalid email address {} to provider {} and emails {}", email,
            samlSource.samlProviderName, samlSource.mustMatchEmailDomains)
          return Response.status(302).location(URI.create(samlFailureUrl)).build()
        }

        val attributes = samlResponse.getAttributes()

        log.trace("SAML attributes are {}", attributes)

        val displayName = attributes.get("urn:oid:2.16.840.1.113730.3.1.241")?.first()
        val firstName = attributes.get("urn:oid:2.5.4.42")?.first()
        val lastName = attributes.get("urn:oid:2.5.4.4")?.first()

        val name = if (displayName == null) {
          if (firstName == null || lastName == null) {
            log.warn("SAML is misconfigured, required attributes are missing: {}", attributes)
            return Response.status(302).location(URI.create(samlMisconfiguredUrl!!)).build()
          }

          "${firstName} ${lastName}"
        } else displayName

        return completionListener.successfulCompletion(
          email, name,
          samlSource.userMustExist, samlFailureUrl, samlSuccessUrl, registrationId
        )
      } else {
        log.warn("SAML login request failed", samlResponse.getValidationException())
      }
    } catch (e: java.lang.Exception) {
      log.error("failed", e)
    }

    return Response.status(302).location(URI.create(samlFailureUrl)).build()
  }

  protected fun matchEmail(email: String, mustMatchEmailDomains: List<String>): Boolean {
    val pos = email.indexOf("@")

    if (pos <= 0 || (pos +1) == email.length) { // bogus email
      return false
    }

    if (mustMatchEmailDomains.isEmpty()) { return true }

    val domain = email.substring(pos+1).lowercase()
    return mustMatchEmailDomains.contains(domain)
  }

  /*
   * This is only used for testing in our circumstance
   */
  @GET
  @Path("/{registrationId}/auth")
  @Prometheus(name = "saml_auth_redirect", help = "SAML Redirect URL")
  fun authRedirect(@PathParam("registrationId") registrationId: String): Response {
    val config =
      samlSources.getSourceFromRegistrationId(registrationId) ?: throw NotAuthorizedException("No such registration id")

    try {
      return Response.status(302).location(URI.create(samlImplProvider.createRequest(config))).build()
    } catch (e: Exception) {
      log.error("Unable to create issuer request", e)
      throw BadRequestException()
    }
  }

  /*
   * This is only used for testing in our circumstance
   */
  @GET
  @Path("/{registrationId}/metadata")
  @Produces(MediaType.TEXT_PLAIN)
  @Prometheus(name = "saml_metadata", help = "SAML Metadata API")
  fun metadata(@PathParam("registrationId") registrationId: String): Response {
    val samlSource =
      samlSources.getSourceFromRegistrationId(registrationId) ?: throw NotAuthorizedException("No such registration id")

    try {
      return Response.status(200).entity(samlSource.saml2Settings().spMetadata).build()
    } catch (e: Exception) {
      throw BadRequestException()
    }
  }
}
