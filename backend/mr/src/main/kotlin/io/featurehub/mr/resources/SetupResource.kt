package io.featurehub.mr.resources

import cd.connect.app.config.ConfigKey
import cd.connect.app.config.DeclaredConfigResolver
import io.featurehub.db.api.*
import io.featurehub.encryption.WebhookEncryptionFeature
import io.featurehub.mr.api.SetupServiceDelegate
import io.featurehub.mr.auth.AuthenticationRepository
import io.featurehub.mr.model.*
import io.featurehub.mr.utils.ConfigurationUtils
import io.featurehub.mr.utils.PortfolioUtils
import io.featurehub.web.security.oauth.AuthProviderCollection
import io.featurehub.web.security.oauth.AuthProviderSource
import jakarta.inject.Inject
import jakarta.ws.rs.BadRequestException
import jakarta.ws.rs.WebApplicationException
import jakarta.ws.rs.core.Response
import org.slf4j.LoggerFactory

class SetupResource @Inject constructor(
  private val setupApi: SetupApi,
  private val authenticationApi: AuthenticationApi,
  private val organizationApi: OrganizationApi,
  private val portfolioApi: PortfolioApi,
  private val groupApi: GroupApi,
  private val authRepository: AuthenticationRepository,
  private val personApi: PersonApi,
  private val portfolioUtils: PortfolioUtils,
  private val authProviderCollection: AuthProviderCollection
) : SetupServiceDelegate {
  @ConfigKey("auth.disable-login")
  protected var loginDisabled: Boolean? = false

  @ConfigKey("ga.tracking-id")
  var googleTrackingId = ""

  init {
    DeclaredConfigResolver.resolve(this)
  }

  override fun isInstalled(): SetupResponse {
    val providerCodes: MutableList<String> = ArrayList(authProviderCollection.codes)
    if (loginDisabled == false) {
      providerCodes.add("local")
    }
    if (setupApi.initialized()) {
      val sr = SetupResponse()
      sr.organization(organizationApi.get())
      sr.providers(providerCodes).providerInfo(fillProviderInfo())
      if ((authProviderCollection.providers.size == 1) && (loginDisabled == true)) { // only 1 external one
        val provider = authProviderCollection.providers[0]
        sr.redirectUrl(provider.redirectUrl)
      }

      sr.capabilityInfo(capabilityInfo())

      return sr
    }
    val setupMissingResponse = SetupMissingResponse()
      .capabilityInfo(java.util.Map.of("trackingId", googleTrackingId))
      .providers(providerCodes)
      .providerInfo(fillProviderInfo())
    throw WebApplicationException(Response.status(Response.Status.NOT_FOUND).entity(setupMissingResponse).build())
  }

  private fun capabilityInfo(): Map<String, String> {
    val enricherEnabled = ConfigurationUtils.enricherEnabled
    val webhooksEnabled = ConfigurationUtils.webhooksEnabled
    val dacha1Enabled = ConfigurationUtils.dacha1Enabled
    val featureGroupsEnabled = ConfigurationUtils.featureGroupsEnabled

    return java.util.Map.of(
      "webhook.features", if (enricherEnabled && webhooksEnabled) "true" else "false",
      "trackingId", googleTrackingId, "dacha1Enabled", if (dacha1Enabled) "true" else "false",
          "featureGroupsEnabled", if (featureGroupsEnabled) "true" else "false",
          "webhook.encryption", if (WebhookEncryptionFeature.isWebhookEncryptionEnabled) "true" else "false",
          "webhook.decryption", if (WebhookEncryptionFeature.isWebhookDecryptionEnabled) "true" else "false",
      "system.config", if (SystemConfigApi.systemConfigEnabled) "true" else "false"
    )
  }

  private fun fillProviderInfo(): Map<String, IdentityProviderInfo> {
    val identityMap: MutableMap<String, IdentityProviderInfo> = HashMap()
    authProviderCollection.providers
      .forEach { am: AuthProviderSource ->
        if (am.authInfo.exposeOnLoginPage && am.authInfo.icon != null) {
          val icon = am.authInfo.icon
          identityMap[am.code] = IdentityProviderInfo()
            .buttonIcon(icon!!.icon)
            .buttonBackgroundColor(icon.buttonBackgroundColor)
            .buttonText(icon.buttonText)
        }
      }
    return identityMap
  }

  override fun setupSiteAdmin(setupSiteAdmin: SetupSiteAdmin): TokenizedPerson {
    if (organizationApi.hasOrganisation()) {
      throw WebApplicationException("duplicate", Response.Status.CONFLICT)
    }

    if (setupSiteAdmin.portfolio.trim { it <= ' ' }.isEmpty()) {
      throw BadRequestException("Portfolio cannot be 0 length")
    }

    if (setupSiteAdmin.organizationName.trim { it <= ' ' }.isEmpty()) {
      throw BadRequestException("Org name cannot be 0 length")
    }

    // if we don't have an email address from them, and they haven't provided an auth provider in the setup request
    // we need to go and figure out what providers are available.
    if (setupSiteAdmin.emailAddress == null) {
      if (setupSiteAdmin.authProvider == null) {
        if (authProviderCollection.codes.isEmpty()) {
          throw BadRequestException("Cannot figure out how to authorise first user, no options provided.")
        }

        if (authProviderCollection.codes.size > 1) {
          throw BadRequestException("Cannot figure out how to authorise first user, too many options provided.")
        }

        setupSiteAdmin.authProvider = authProviderCollection.codes[0]
      }

      val ap = authProviderCollection.find(setupSiteAdmin.authProvider!!)

      createOrganization(setupSiteAdmin)

      // they are using an external provider, so we can create the org and portfolio, but that is all.
      return if (ap != null) {
        createPortfolio(setupSiteAdmin, null)
        TokenizedPerson().redirectUrl(ap.redirectUrl)
      } else {
        throw BadRequestException("Unknown auth provider") // invalid attempt to set up
      }
    }

    // normal non-external provider flow
    val organization = createOrganization(setupSiteAdmin)

    // create them
    try {
      personApi.create(setupSiteAdmin.emailAddress!!, "Admin", null)
    } catch (e: PersonApi.DuplicatePersonException) {
      throw WebApplicationException(Response.status(Response.Status.CONFLICT).build())
    }

    // now register them
    var person = authenticationApi.register(
      setupSiteAdmin.name,
      setupSiteAdmin.emailAddress!!,
      setupSiteAdmin.password,
      null
    )
    createPortfolio(setupSiteAdmin, person)

    //create the group and add admin to the group - any preference on group name here?
    val group = groupApi.createOrgAdminGroup(organization.id!!, "org_admin", person!!)
    groupApi.addPersonToGroup(group!!.id, person.id!!.id, Opts.empty())
    person = personApi[person.id!!.id, Opts.opts(FillOpts.Groups, FillOpts.Acls)]

    // the capability needing to be returned is a unique problem of using username/password and
    // returning a token which is immediately usable.
    return TokenizedPerson().accessToken(authRepository.put(person)).person(person).capabilityInfo(capabilityInfo())
  }

  private fun createPortfolio(setupSiteAdmin: SetupSiteAdmin, person: Person?) {
    //this should create portfolio
    try {
      val portfolio = portfolioApi.createPortfolio(
        CreatePortfolio().name(setupSiteAdmin.portfolio).description(setupSiteAdmin.portfolio),
        Opts.empty(),
        person?.id!!.id
      )!!

      groupApi.createGroup(
        portfolio.id,
        CreateGroup().name(portfolioUtils.formatPortfolioAdminGroupName(portfolio.name)).admin(true), person
      )
    } catch (e: PortfolioApi.DuplicatePortfolioException) {
      log.error("Duplicate portfolio name or group", e)
      throw WebApplicationException(Response.Status.CONFLICT)
    } catch (e: GroupApi.DuplicateGroupException) {
      log.error("Duplicate portfolio name or group", e)
      throw WebApplicationException(Response.Status.CONFLICT)
    }
  }

  protected fun createOrganization(setupSiteAdmin: SetupSiteAdmin): Organization {
    // this should create the organisation
    return organizationApi.save(
      Organization()
        .name(setupSiteAdmin.organizationName)
    )
  }

  companion object {
    private val log = LoggerFactory.getLogger(SetupResource::class.java)
  }
}
