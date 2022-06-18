package io.featurehub.mr.rest

import io.featurehub.db.api.AuthenticationApi
import io.featurehub.db.api.PersonApi
import io.featurehub.mr.auth.AuthManagerService
import io.featurehub.mr.auth.AuthenticationRepository
import io.featurehub.mr.model.PasswordReset
import io.featurehub.mr.model.Person
import io.featurehub.mr.model.PersonId
import io.featurehub.mr.resources.AuthResource
import io.featurehub.web.security.oauth.AuthProviderCollection
import jakarta.ws.rs.ForbiddenException
import jakarta.ws.rs.NotFoundException
import jakarta.ws.rs.core.SecurityContext
import spock.lang.Specification

class AuthenticationResourceSpec extends Specification {
  PersonApi personApi
  AuthManagerService authManager
  AuthenticationApi authApi
  AuthResource resource
  AuthenticationRepository authRepository
  Person fromPerson
  AuthProviderCollection authProviderCollection

  def setup() {
    personApi = Mock(PersonApi)
    authManager = Mock(AuthManagerService)
    authApi = Mock(AuthenticationApi)
    authRepository = Mock(AuthenticationRepository)
    authProviderCollection = Mock(AuthProviderCollection)
    fromPerson = new Person().id(new PersonId().id(UUID.randomUUID()))
    authManager.from(_) >> fromPerson

    resource = new AuthResource(authApi, authManager, personApi, authRepository, authProviderCollection)
  }

  def "A non-admin cannot reset a password"() {
    when:
      resource.resetPassword(UUID.randomUUID(), new PasswordReset(), null)
    then:
      thrown ForbiddenException
  }

  def "An admin can reset the password for a person"() {
    given: "i am an admin"
      authManager.isAnyAdmin(_) >> true
    and:
      Person p = new Person()
    when: "i try and reset a password"
      Person newPerson = resource.resetPassword(fromPerson.id.id, new PasswordReset(password: 'fred'), null)
    then:
      newPerson == p
      1 * authApi.resetPassword(fromPerson.id.id, 'fred', fromPerson.id.id, false) >> p
  }

  def "An admin trying to reset a password for an unknown person will get 404"() {
    given: "i am an admin"
      authManager.isAnyAdmin(_) >> true
      UUID pId = UUID.randomUUID()
    and: "the reset process returns a person"
      authApi.resetPassword(pId, null, UUID.randomUUID(), false) >> null
    when: "i try and reset a password"
      resource.resetPassword(pId, new PasswordReset(password: 'rrrrr'), null)
    then:
      thrown NotFoundException
  }

  def "a person has to be in password reset mode to be allowed to replace password"() {
    when: "i call as a non reset person"
      UUID pId = UUID.randomUUID()
      resource.replaceTempPassword(pId, new PasswordReset(), null)
    then:
      thrown ForbiddenException
  }

  def "a person can only replace the password of themselves"() {
    given:
      fromPerson.passwordRequiresReset(true)
      UUID pId = UUID.randomUUID()
    when:
      resource.replaceTempPassword(pId, new PasswordReset(), null)
    then:
      thrown ForbiddenException
  }

  def "a person who replaces their password will have their session invalidated and a new token issued"() {
    given:
      fromPerson.passwordRequiresReset(true)
    and:
      Person newPerson = new Person()
    and:
      SecurityContext ctx = Mock(SecurityContext)
    when:
      def tp = resource.replaceTempPassword(fromPerson.id.id, new PasswordReset(password: 'ibiza'), ctx)
    then:
      authApi.replaceTemporaryPassword(fromPerson.id.id, 'ibiza') >> newPerson
      authRepository.invalidate(ctx)
      authRepository.put(newPerson) >> "fred"
      tp.accessToken == "fred"
      tp.person == newPerson
  }
}
