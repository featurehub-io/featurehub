package io.featurehub.db.services

import io.ebean.annotation.Transactional
import io.featurehub.db.api.*
import io.featurehub.db.api.GroupApi.DuplicateGroupException
import io.featurehub.db.api.PersonApi.DuplicatePersonException
import io.featurehub.mr.model.*
import io.featurehub.mr.utils.PortfolioUtils
import jakarta.inject.Inject
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * This is in this package so ebean can use it to scan it for transactions. We need to wrap a transaction
 * around the whole process so it works as a unit and it ensures the same connection is used via database
 * access so we don't get requests split across HA.
 */

interface InternalOAuthPersonCreation {
  fun createUser(email: String, username: String): Person?
}

class OAuthPersonCreation @Inject constructor(
  private val personApi: PersonApi,
  private val authenticationApi: AuthenticationApi,
  private val organizationApi: OrganizationApi,
  private val groupApi: GroupApi,
  private val portfolioApi: PortfolioApi,
  private val portfolioUtils: PortfolioUtils
) : InternalOAuthPersonCreation {
  private val log: Logger = LoggerFactory.getLogger(OAuthPersonCreation::class.java)

  @Transactional
  override fun createUser(email: String, username: String): Person? {
    // determine if they were the first user, and if so, complete setup
    val firstUser: Boolean = personApi.noUsersExist()

    // first we create them, this will give them a token and so forth, we are playing with existing functionality
    // here
    try {
      personApi.create(email, username, null)
    } catch (e: DuplicatePersonException) {
      log.error("Shouldn't get here, as we check if the person exists before creating them.")
      return null
    }

    // now "register" them. We can provide a null password OK, it just ignores it, but this removes
    // any registration token required
    val person: Person = authenticationApi.register(username, email, null, null)
    if (firstUser) {
      val organization: Organization = organizationApi.get()
      // create the superuser group and add admin to the group -
      val group: Group = groupApi.createOrgAdminGroup(organization.id, "org_admin", person)
      groupApi.addPersonToGroup(group.id, person.id!!.id, Opts.empty())

      // find the only portfolio and update its members to include this one
      val portfolio: Portfolio = portfolioApi.findPortfolios(
        null, SortOrder.ASC,
        Opts.empty(), person
      ).first()

      try {
        groupApi.createPortfolioGroup(
          portfolio.id,
          Group().name(portfolioUtils.formatPortfolioAdminGroupName(portfolio)).admin(true), person
        )
      } catch (e: DuplicateGroupException) {
        log.error("If we have this exception, the site is broken.", e)
      }
    }
    return person
  }
}
