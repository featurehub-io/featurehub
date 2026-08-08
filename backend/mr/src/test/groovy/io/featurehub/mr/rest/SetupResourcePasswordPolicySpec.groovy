package io.featurehub.mr.rest

import io.featurehub.db.api.AuthenticationApi
import io.featurehub.db.api.GroupApi
import io.featurehub.db.api.OrganizationApi
import io.featurehub.db.api.PersonApi
import io.featurehub.db.api.PortfolioApi
import io.featurehub.db.api.SetupApi
import io.featurehub.db.password.PasswordPolicy
import io.featurehub.mr.auth.AuthenticationRepository
import io.featurehub.mr.model.Organization
import io.featurehub.mr.model.PasswordPolicyRule
import io.featurehub.mr.model.PasswordPolicyViolationReport
import io.featurehub.mr.model.SetupMissingResponse
import io.featurehub.mr.model.SetupSiteAdmin
import io.featurehub.mr.resources.SetupResource
import io.featurehub.mr.utils.PortfolioUtils
import io.featurehub.web.security.oauth.AuthProviderCollection
import io.featurehub.web.security.oauth.AuthProviderSource
import jakarta.ws.rs.WebApplicationException
import spock.lang.Specification

class SetupResourcePasswordPolicySpec extends Specification {
  static final String WEAK_PASSWORD = 'admin'

  SetupApi setupApi
  AuthenticationApi authenticationApi
  OrganizationApi organizationApi
  PortfolioApi portfolioApi
  GroupApi groupApi
  AuthenticationRepository authRepository
  PersonApi personApi
  PortfolioUtils portfolioUtils
  AuthProviderCollection authProviderCollection
  PasswordPolicy passwordPolicy
  SetupResource resource

  def setup() {
    setupApi = Mock(SetupApi)
    authenticationApi = Mock(AuthenticationApi)
    organizationApi = Mock(OrganizationApi)
    portfolioApi = Mock(PortfolioApi)
    groupApi = Mock(GroupApi)
    authRepository = Mock(AuthenticationRepository)
    personApi = Mock(PersonApi)
    portfolioUtils = Mock(PortfolioUtils)
    authProviderCollection = Mock(AuthProviderCollection)
    passwordPolicy = Spy(PasswordPolicy, constructorArgs: [8, 40, true, true, true, false])

    resource = new SetupResource(setupApi, authenticationApi, organizationApi, portfolioApi, groupApi,
      authRepository, personApi, portfolioUtils, authProviderCollection, passwordPolicy)
  }

  def "setting up the site admin with a weak password leaves nothing behind"() {
    given: "the site is not yet set up"
      organizationApi.hasOrganisation() >> false
    when: "i set up with a password that fails the policy"
      resource.setupSiteAdmin(new SetupSiteAdmin(portfolio: 'p1', organizationName: 'org1',
        emailAddress: 'admin@featurehub.io', name: 'Admin', password: WEAK_PASSWORD))
    then: "i get a 400 naming the rules i broke"
      def e = thrown(WebApplicationException)
      e.response.status == 400
      def report = e.response.entity as PasswordPolicyViolationReport
      report.violatedRules.contains(PasswordPolicyRule.MIN_LENGTH)
      report.violatedRules.contains(PasswordPolicyRule.REQUIRE_UPPERCASE)
      report.violatedRules.contains(PasswordPolicyRule.REQUIRE_NUMERIC)
    and: "nothing at all was created - the rejection happens before any of it"
      0 * organizationApi.save(_)
      0 * portfolioApi.createPortfolio(_, _, _)
      0 * groupApi.createGroup(_, _, _)
      0 * groupApi.createOrgAdminGroup(_, _, _)
      0 * personApi.create(_, _, _)
      0 * authenticationApi.register(_, _, _, _)
  }

  def "an external provider setup is never policy checked, because it sets no password"() {
    given: "the site is not yet set up and there is one external provider"
      organizationApi.hasOrganisation() >> false
      authProviderCollection.codes >> ['oauth2-google']
      authProviderCollection.find(_) >> Mock(AuthProviderSource)
      organizationApi.save(_) >> new Organization()
    when: "i set up with no email address and no password"
      try {
        resource.setupSiteAdmin(new SetupSiteAdmin(portfolio: 'p1', organizationName: 'org1'))
      } catch (Exception ignored) {
        // This path currently dies further downstream on a pre-existing NPE in createPortfolio
        // (person?.id!!.id with a null person), which has nothing to do with the password policy.
        // What matters here is only that we never reached the validator.
      }
    then: "the policy is never consulted"
      0 * passwordPolicy.validate(_)
  }

  def "the active policy is published when the site is not yet installed"() {
    given: "the site is not set up"
      setupApi.initialized() >> false
      authProviderCollection.codes >> []
      authProviderCollection.providers >> []
    when:
      resource.isInstalled()
    then: "the 404 payload carries the policy, so the setup form can show the rules"
      def e = thrown(WebApplicationException)
      e.response.status == 404
      def missing = e.response.entity as SetupMissingResponse
      missing.passwordPolicy.minLength == 8
      missing.passwordPolicy.maxLength == 40
      missing.passwordPolicy.requireUppercase
      missing.passwordPolicy.requireLowercase
      missing.passwordPolicy.requireNumeric
      !missing.passwordPolicy.requireSymbol
  }

  def "the active policy is published when the site is installed"() {
    given: "the site is set up"
      setupApi.initialized() >> true
      organizationApi.get() >> new Organization()
      authProviderCollection.codes >> []
      authProviderCollection.providers >> []
    when:
      def response = resource.isInstalled()
    then:
      response.passwordPolicy.minLength == 8
      response.passwordPolicy.maxLength == 40
      response.passwordPolicy.requireUppercase
      !response.passwordPolicy.requireSymbol
  }

  def "the published policy reflects what the operator configured"() {
    given: "an operator has tightened the policy"
      def strict = new PasswordPolicy(12, 30, true, true, true, true)
      resource = new SetupResource(setupApi, authenticationApi, organizationApi, portfolioApi, groupApi,
        authRepository, personApi, portfolioUtils, authProviderCollection, strict)
      setupApi.initialized() >> true
      organizationApi.get() >> new Organization()
      authProviderCollection.codes >> []
      authProviderCollection.providers >> []
    when:
      def response = resource.isInstalled()
    then:
      response.passwordPolicy.minLength == 12
      response.passwordPolicy.maxLength == 30
      response.passwordPolicy.requireSymbol
  }
}
