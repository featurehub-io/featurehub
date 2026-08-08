package io.featurehub.mr.rest

import io.featurehub.db.api.AuthenticationApi
import io.featurehub.db.api.PersonApi
import io.featurehub.db.password.PasswordPolicy
import io.featurehub.mr.auth.AuthManagerService
import io.featurehub.mr.auth.AuthenticationRepository
import io.featurehub.mr.model.PasswordPolicyRule
import io.featurehub.mr.model.PasswordPolicyViolationReport
import io.featurehub.mr.model.PasswordReset
import io.featurehub.mr.model.PasswordUpdate
import io.featurehub.mr.model.Person
import io.featurehub.mr.model.PersonId
import io.featurehub.mr.model.PersonRegistrationDetails
import io.featurehub.mr.resources.AuthResource
import io.featurehub.web.security.oauth.AuthProviderCollection
import jakarta.ws.rs.ForbiddenException
import jakarta.ws.rs.WebApplicationException
import jakarta.ws.rs.core.SecurityContext
import spock.lang.Specification

/**
 * The policy is enforced at this layer, not in the persistence layer, so this is where it has to be
 * proven - on every path that sets a password.
 */
class AuthResourcePasswordPolicySpec extends Specification {
  static final String WEAK_PASSWORD = 'letmein'
  static final String GOOD_PASSWORD = 'Passw0rdX'

  PersonApi personApi
  AuthManagerService authManager
  AuthenticationApi authApi
  AuthenticationRepository authRepository
  AuthProviderCollection authProviderCollection
  AuthResource resource
  Person fromPerson

  def setup() {
    personApi = Mock(PersonApi)
    authManager = Mock(AuthManagerService)
    authApi = Mock(AuthenticationApi)
    authRepository = Mock(AuthenticationRepository)
    authProviderCollection = Mock(AuthProviderCollection)

    fromPerson = new Person().id(new PersonId().id(UUID.randomUUID()))
    authManager.from(_) >> fromPerson

    resource = new AuthResource(authApi, authManager, personApi, authRepository, authProviderCollection,
      new PasswordPolicy(8, 40, true, true, true, false))
  }

  private static PasswordPolicyViolationReport reportFrom(WebApplicationException e) {
    assert e.response.status == 400
    return e.response.entity as PasswordPolicyViolationReport
  }

  def "registering with a weak password is rejected and the registration is not completed"() {
    given: "a person exists against the registration token"
      def token = 'a-registration-token'
      def person = new Person().id(new PersonId().id(UUID.randomUUID())).email('bob@featurehub.io')
      personApi.getByToken(token, _) >> person
    when: "i register with a password that fails the policy"
      resource.registerPerson(new PersonRegistrationDetails(name: 'Bob', email: 'bob@featurehub.io',
        password: WEAK_PASSWORD, confirmPassword: WEAK_PASSWORD, registrationToken: token))
    then: "i get a 400 naming the rules i broke"
      def e = thrown(WebApplicationException)
      def report = reportFrom(e)
      report.violatedRules.contains(PasswordPolicyRule.REQUIRE_UPPERCASE)
      report.violatedRules.contains(PasswordPolicyRule.REQUIRE_NUMERIC)
      report.minLength == 8
    and: "the registration never happened, so the token is still usable"
      0 * authApi.register(_, _, _, _)
  }

  def "registering with a compliant password succeeds and issues a token"() {
    given:
      def token = 'a-registration-token'
      def person = new Person().id(new PersonId().id(UUID.randomUUID())).email('bob@featurehub.io')
      def registered = new Person().id(new PersonId().id(UUID.randomUUID())).email('bob@featurehub.io')
      personApi.getByToken(token, _) >> person
      authApi.register('Bob', 'bob@featurehub.io', GOOD_PASSWORD, null) >> registered
      authRepository.put(_) >> 'access-token'
    when:
      def tp = resource.registerPerson(new PersonRegistrationDetails(name: 'Bob', email: 'bob@featurehub.io',
        password: GOOD_PASSWORD, confirmPassword: GOOD_PASSWORD, registrationToken: token))
    then:
      tp.person == registered
      tp.accessToken == 'access-token'
  }

  def "changing your own password to a weak one is rejected and nothing is stored"() {
    when: "i supply the correct old password but a weak new one"
      resource.changePassword(fromPerson.id.id,
        new PasswordUpdate(oldPassword: 'my-current-password', newPassword: WEAK_PASSWORD), null)
    then:
      def e = thrown(WebApplicationException)
      reportFrom(e).violatedRules.contains(PasswordPolicyRule.REQUIRE_UPPERCASE)
    and: "the password was never changed"
      0 * authApi.changePassword(_, _, _)
  }

  def "replacing a temporary password with a weak one is rejected and the reset flag stays set"() {
    given: "i am in password reset mode"
      fromPerson.passwordRequiresReset(true)
    when:
      resource.replaceTempPassword(fromPerson.id.id, new PasswordReset(password: WEAK_PASSWORD), Mock(SecurityContext))
    then:
      def e = thrown(WebApplicationException)
      reportFrom(e).violatedRules.contains(PasswordPolicyRule.REQUIRE_NUMERIC)
    and: "the temporary password was not replaced, so a reset is still required"
      0 * authApi.replaceTemporaryPassword(_, _)
      fromPerson.passwordRequiresReset
  }

  def "an admin cannot reset someone's password to a weak temporary one"() {
    given: "i am an admin"
      authManager.isAnyAdmin(_) >> true
    when:
      resource.resetPassword(UUID.randomUUID(), new PasswordReset(password: WEAK_PASSWORD), null)
    then:
      def e = thrown(WebApplicationException)
      reportFrom(e).violatedRules.contains(PasswordPolicyRule.REQUIRE_UPPERCASE)
    and:
      0 * authApi.resetPassword(_, _, _, _)
  }

  def "a non admin resetting a password is forbidden and learns nothing about the policy"() {
    given: "i am not an admin"
      authManager.isAnyAdmin(_) >> false
    when: "i try to reset someone's password with a weak one"
      resource.resetPassword(UUID.randomUUID(), new PasswordReset(password: WEAK_PASSWORD), null)
    then: "i get a bare 403, not a policy violation report"
      thrown(ForbiddenException)
    and:
      0 * authApi.resetPassword(_, _, _, _)
  }

  def "an over long legacy password can still be used to change to a compliant one"() {
    given: "my stored password is longer than the policy maximum"
      def legacyPassword = 'x' * 60
      def changed = new Person()
    when: "i supply it as my old password with a compliant new one"
      def result = resource.changePassword(fromPerson.id.id,
        new PasswordUpdate(oldPassword: legacyPassword, newPassword: GOOD_PASSWORD), null)
    then: "the old password is not policed, only the new one"
      1 * authApi.changePassword(fromPerson.id.id, legacyPassword, GOOD_PASSWORD) >> changed
      result == changed
  }
}
