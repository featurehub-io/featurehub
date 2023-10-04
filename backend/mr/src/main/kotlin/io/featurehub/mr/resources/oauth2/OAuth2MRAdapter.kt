package io.featurehub.mr.resources.oauth2

import cd.connect.app.config.ConfigKey
import cd.connect.app.config.DeclaredConfigResolver
import io.featurehub.db.api.*
import io.featurehub.mr.auth.AuthenticationRepository
import io.featurehub.mr.model.*
import io.featurehub.mr.utils.PortfolioUtils
import io.featurehub.web.security.oauth.SSOCompletionListener
import jakarta.inject.Inject
import jakarta.inject.Singleton
import jakarta.ws.rs.core.Cookie
import jakarta.ws.rs.core.NewCookie
import jakarta.ws.rs.core.Response
import org.slf4j.LoggerFactory
import java.net.URI

@Singleton
class OAuth2MRAdapter @Inject constructor(
  protected val personApi: PersonApi,
  private val authenticationApi: AuthenticationApi,
  private val portfolioApi: PortfolioApi,
  private val groupApi: GroupApi,
  private val authRepository: AuthenticationRepository,
  private val portfolioUtils: PortfolioUtils,
  private val organizationApi: OrganizationApi
) : SSOCompletionListener {
  @ConfigKey("oauth2.cookie.domain")
  var cookieDomain: String? = ""

  @ConfigKey("oauth2.cookie.https-only")
  var cookieSecure: Boolean? = false

  init {
    DeclaredConfigResolver.resolve(this)
  }

  override fun successfulCompletion(
    email: String, username: String?, userMustBeCreatedFirst: Boolean,
    failureUrl: String, successUrl: String, provider: String
  ): Response {
    // discover if they are a user and if not, add them
    var p = personApi[email, Opts.opts(FillOpts.Archived)]
    if (p != null && p.whenArchived != null) {
      log.warn("User {} attempted to login and have been deleted.", email)
      return Response.status(302).location(URI.create(failureUrl)).build()
    }
    if (p == null) {
      // if the user must be created in the database before they are allowed to sign in, redirect to failure.
      if (userMustBeCreatedFirst) {
        log.warn("User {} attempted to login and they aren't in the database and they need to be.", email)
        return Response.status(302).location(URI.create(failureUrl)).build()
      }
      p = createUser(email, username)
    } else {
      p.name = username

      try {
        personApi.updateV2(p.id!!.id, UpdatePerson().name(username), p.id!!.id)
      } catch (ignored: OptimisticLockingException) {
      }

      authenticationApi.updateLastAuthenticated(p.id!!.id)
    }

    // store user in session with bearer token
    val token = authRepository.put(p)
    val uri = URI.create(successUrl)
    // add cookie
    return Response.status(Response.Status.FOUND).cookie(
      NewCookie.Builder("bearer-token").value(token).path("/")
        .domain(if (cookieDomain!!.isEmpty()) null else cookieDomain)
        .version(Cookie.DEFAULT_VERSION)
        .maxAge(NewCookie.DEFAULT_MAX_AGE)
        .secure(cookieSecure!!)
        .httpOnly(false).build()
    )
      .location(uri).build()
  }

  private fun createUser(email: String?, username: String?): Person? {
    // determine if they were the first user, and if so, complete setup
    val firstUser = personApi.noUsersExist()

    // first we create them, this will give them a token and so forth, we are playing with existing functionality
    // here
    try {
      personApi.create(email!!, username, null)
    } catch (e: PersonApi.DuplicatePersonException) {
      log.error("Shouldn't get here, as we check if the person exists before creating them.")
      return null
    }

    // now "register" them. We can provide a null password OK, it just ignores it, but this removes
    // any registration token required
    val person = authenticationApi.register(username, email, null, null) ?: return null

    if (firstUser) {
      val organization = organizationApi.get()
      // create the superuser group and add admin to the group -
      val group = groupApi.createOrgAdminGroup(organization.id, "org_admin", person)
      groupApi.addPersonToGroup(group!!.id, person.id!!.id, Opts.empty())

      // find the only portfolio and update its members to include this one
      val portfolio = portfolioApi.findPortfolios(
        null, SortOrder.ASC,
        Opts.empty(), person.id!!.id
      )[0]
      try {
        val name = portfolioUtils.formatPortfolioAdminGroupName(portfolio.name)

        groupApi.createGroup(
          portfolio.id,
          CreateGroup().name(name).admin(true), person
        )
      } catch (e: GroupApi.DuplicateGroupException) {
        log.error("If we have this exception, the site is broken.", e)
      }
    }
    return person
  }

  override fun initialAppSetupComplete(): Boolean {
    return organizationApi.get() != null
  }

  companion object {
    private val log = LoggerFactory.getLogger(OAuth2MRAdapter::class.java)
  }
}
