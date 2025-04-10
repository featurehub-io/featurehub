package io.featurehub.db.services

import io.featurehub.db.api.DBLoginSession
import io.featurehub.db.api.Opts
import io.featurehub.mr.events.common.CacheSource
import io.featurehub.mr.model.Application
import io.featurehub.mr.model.ApplicationGroupRole
import io.featurehub.mr.model.ApplicationRoleType
import io.featurehub.mr.model.CreateApplication
import io.featurehub.mr.model.CreateGroup
import io.featurehub.mr.model.CreatePortfolio
import io.featurehub.mr.model.Group
import io.featurehub.mr.model.Person
import io.featurehub.mr.model.PersonId
import io.featurehub.mr.model.Portfolio
import org.apache.commons.lang3.RandomStringUtils
import spock.lang.Shared

import java.time.Instant

class AuthenticationSpec extends BaseSpec {
  @Shared AuthenticationSqlApi auth
  @Shared PersonSqlApi personApi
  @Shared ApplicationSqlApi appApi
  @Shared PortfolioSqlApi portfolioApi


  def setupSpec() {
    baseSetupSpec()
    auth = new AuthenticationSqlApi(convertUtils)
    personApi = new PersonSqlApi(database, convertUtils, archiveStrategy, Mock(InternalGroupSqlApi))
    portfolioApi = new PortfolioSqlApi(database, convertUtils, archiveStrategy)
    appApi = new ApplicationSqlApi(convertUtils, Mock(CacheSource), archiveStrategy, Mock(InternalFeatureApi))
  }

  def "I should be able to register a new user"() {
    when: "i register"
      personApi.create('william@featurehub.io', "William", null)
      Person person = auth.register("william", "william@featurehub.io", "yacht", null)
    then:
      person.id
      person.name == "william"
      person.email == "william@featurehub.io"
      !person.passwordRequiresReset
  }

  def "I should be able to invalidate a user and their login token should not longer be valid"() {
    given: "i register a new user"
      def email = "${RandomStringUtils.randomAlphabetic(10)}@mailinator.com"
      personApi.create(email, email, null)
      Person person = auth.register(email, email, "yacht", null)
    when: "i create a session"
      def token = RandomStringUtils.randomAlphabetic(20)
      def session = auth.createSession(new DBLoginSession(person, token, Instant.now()))
    then: "the session is valid"
      auth.findSession(token) != null
    when: "i delete person"
      def couldDelete = personApi.delete(email, true)
    then: "the session is no longer valid"
      couldDelete
      auth.findSession(token) == null
  }

  def "I should be able to invalidate a user and not log in as them again"() {
    given: "i register"
      def email = "seth@featurehub.io"
      personApi.create(email, email, null)
      Person person = auth.register(email, email, "yacht", null)
    and:
      def l = auth.login(email, "yacht")
    when:
      def couldDelete = personApi.delete(email, true)
    then:
      l != null
      couldDelete
      auth.login(email, "yacht") == null
  }

  def "I cannot register twice"() {
    when: "i register"
      personApi.create('william-double-reg@featurehub.io', "William", null)
      Person person = auth.register("william", "william-double-reg@featurehub.io", "yacht", null)
    and: "i try again"
      Person person2 = auth.register("william", "william-double-reg@featurehub.io", "yacht", null)
    then:
      person != null
      person2 == null
  }

  def "I should be able to login"() {
    given: "i register"
      personApi.create('william1@featurehub.io', "William", null)
      Person person = auth.register("william", "william1@featurehub.io", "bathroom", null)
    when: "i login"
      Person loginPerson = auth.login("william1@featurehub.io", "bathroom")
    then:
      loginPerson
  }

  def "I should be able to login and change my password"() {
    given: "i register"
      personApi.create('william4@featurehub.io', "William", null)
      Person person = auth.register("william", "william4@featurehub.io", "bathroom", null)
    and: "i login"
      Person loginPerson = auth.login("william4@featurehub.io", "bathroom")
    when: "i change my password"
      auth.changePassword(loginPerson.id.id, "bathroom", "honey")
    and: "login"
      Person loginPerson2 = auth.login("william4@featurehub.io", "honey")
    then: "i still can"
      loginPerson2
      loginPerson2.id == loginPerson.id
  }

  def "i cannot register with an empty password"() {
    when:
      personApi.create('william-empty-pw@featurehub.io',"William", null)
      Person person = auth.register("william", "william-empty-pw@featurehub.io", "", null)
    then:
      person == null
  }

  def "i cannot change my password to an empty password"() {
    given: "i register"
      personApi.create('william-emptypw-change@featurehub.io', "William", null)
      Person person = auth.register("william", "william-emptypw-change@featurehub.io", "bathroom", null)
    when: "i change my password using the wrong one"
      Person changedPerson = auth.changePassword(person.id.id, "bathroom", "")
    then:
      changedPerson == null
  }

  def "i shouldn't be able to change my password if the old one is wrong"() {
    given: "i register"
      personApi.create('william-badpw@featurehub.io', "William", null)
      Person person = auth.register("william", "william-badpw@featurehub.io", "bathroom", null)
    and: "i login"
      Person loginPerson = auth.login("william-badpw@featurehub.io", "bathroom")
    when: "i change my password using the wrong one"
      Person changedPerson = auth.changePassword(loginPerson.id.id, "yacht", "honey")
    and: "i try and login"
      Person loginPerson2 = auth.login("william-badpw@featurehub.io", "honey")
    then:
      !loginPerson2
      !changedPerson
  }

  def "a superuser can change a person's password"() {
    given: "i register"
      personApi.create('william2@featurehub.io', "William", null)
      Person person = auth.register("william2", "william2@featurehub.io", "yacht", null)
    when: "the super user changes my password"
      Person resetPerson = auth.resetPassword(person.id.id, "honey", superuser, false)
    then:
      resetPerson
      resetPerson.email == 'william2@featurehub.io'
  }

  def "a newly registered user has forgotten their password"() {
    given: "i register"
      personApi.create('william3@featurehub.io', "William", null)
      Person person = auth.register("william", "william3@featurehub.io", "bathroom", null)
    and: "i login"
      Person loginPerson = auth.login("william3@featurehub.io", "bathroom")
    when: "the super user changes my password"
      auth.resetPassword(person.id.id, "honey", superuser, false)
    and: "i try and login "
      Person newPerson = auth.login("william3@featurehub.io", "honey")
    then: "my password requires resetting"
      newPerson.passwordRequiresReset
  }

  def "a superuser changes my password, i reset it and i can't reset it again"() {
    given: "i register"
      personApi.create('william-temp@featurehub.io', "William", null)
      Person person = auth.register("william", "william-temp@featurehub.io", "bathroom", null)
    and: "the super user changes my password"
      auth.resetPassword(person.id.id, "honey", superuser, false)
    and: "i reset my password"
      auth.replaceTemporaryPassword(person.id.id, "buoy")
    and: "i try and reset it again"
      Person reset = auth.replaceTemporaryPassword(person.id.id, "buoy")
    when: "i login"
      Person newPerson = auth.login("william-temp@featurehub.io", "buoy")
    then: "my password does not require reset"
      !newPerson.passwordRequiresReset
      reset == null
  }

  def "a person cannot reset their own password"() {
    given: "i register"
      def createdPerson = personApi.create('william-reset@featurehub.io', "William", null)
      Person person = auth.register("william", "william-reset@featurehub.io", "bathroom", null)
    when: "i try and reset my own password"
      Person resetUser = auth.resetPassword(createdPerson.id, "honey", person.id.id, false)
    then: "the system prevents me"
      !resetUser
  }

  def "i cannot reset my password to an empty password"() {
    given: "i register"
      personApi.create('william-reset1@featurehub.io',"William", null)
      Person person = auth.register("william", "william-reset1@featurehub.io", "bathroom", null)
    when: "i try and reset my password to empty"
      Person resetUser = auth.resetPassword(person.id.id, "", superuser, false)
    then: "the system prevents me"
      !resetUser
  }

  def "i can reset another user's password"() {
    given: "i register"
      personApi.create('william-reset2@featurehub.io', "William",superuser)
      personApi.create('william-reset3@featurehub.io', "William",null)
      Person p2 = auth.register("william", "william-reset2@featurehub.io", "bathroom", null)
      Person p3 = auth.register("william", "william-reset3@featurehub.io", "bathroom", null)
    when: "i try and reset their password"
      Person resetUser = auth.resetPassword(p3.id.id, "bath2", superuser, false)
      Person resetUser2 = auth.resetPassword(p2.id.id, "bath2", superuser, false)
    then:
      resetUser
  }

  def "A user who is a portfolio manager gets their application roles when they login"() {
    given: "i register"
      personApi.create('portman26@mailinator.com', "Portman26",superuser)
      Person p2 = auth.register("william", "portman26@mailinator.com", "hooray", null)
    and: "i create a new portfolio"
      Portfolio portfolio1 = portfolioApi.createPortfolio(new CreatePortfolio().name("persontestportfolio"), Opts.empty(), superuser)
    and: "i create an application in that portfolio"
      def app1 = appApi.createApplication(portfolio1.id, new CreateApplication().name("persontest-app1").description("some desc"), superPerson)
    and: "i make the user a portfolio manager"
      def portfolioGroup = groupSqlApi.createGroup(portfolio1.id,
        new CreateGroup().name("admin-group").admin(true)
          .applicationRoles([new ApplicationGroupRole().applicationId(app1.id)
                               .roles([ApplicationRoleType.FEATURE_EDIT])]), superPerson)
      groupSqlApi.updateGroup(portfolioGroup.id, portfolioGroup.members([p2]), null,
        true, false, false, Opts.empty())
    when: "i login"
      def user = auth.login(p2.email, "hooray")
    then: "i have the application role permission to the portfolio"
      user.groups.find({it.applicationRoles.find({ar -> ar.roles.contains(ApplicationRoleType.FEATURE_EDIT) && ar.applicationId == app1.id})})
  }

  def "We can create a session for a person and then find their session by token"() {
    given: "i register"
      def email = 'portman27@mailinator.com'
      personApi.create(email, "Portman27",superuser)
      Person p2 = auth.register("william", email, "hooray", null)
    and: 'a defined session'
      def originalSession = new DBLoginSession(p2, "token", Instant.now())
    when: 'i create the session'
      def session = auth.createSession(originalSession)
    and: "then find the session"
      def foundSession = auth.findSession(originalSession.token)
    and: "then invalidate the session"
      auth.invalidateSession("token")
      def invalidSession = auth.findSession(originalSession.token)
    then:
      session == originalSession
      foundSession.token == originalSession.token
      foundSession.lastSeen == originalSession.lastSeen
      foundSession.person.id.id == p2.id.id
      foundSession.person.email == p2.email
      invalidSession == null
  }

  def "I can't create a session with missing person details"() {
    when: 'i create a session with no token'
      auth.createSession(new DBLoginSession(new Person(id: new PersonId(id: null)), "tok", Instant.now()))
    then:
      thrown IllegalArgumentException
  }

  def "an invalid person will create a null session"() {
    when: 'i create a session with no token'
      def session = auth.createSession(new DBLoginSession(new Person(id: new PersonId(id: UUID.randomUUID())), "tok", Instant.now()))
    then:
      session == null
  }

  def "when i try and reset an expired token that has no token i get no result"() {
    given: "i register"
      def email = 'portman28@mailinator.com'
      personApi.create(email, "Portman28",superuser)
    and: "i register them so their token goes away"
      Person p2 = auth.register("william", email, "hooray", null)
    when: "i try and reset the token"
      def reset = auth.resetExpiredRegistrationToken(email)
    then:
      reset == null
  }

  def "when i try and reset an expired token that has a token we get a new token"() {
    given: "i register"
      def email = 'portman29@mailinator.com'
      def token = personApi.create(email, "Portman29",superuser)
    when: "i try and reset the token"
      def reset = auth.resetExpiredRegistrationToken(email)
    then:
      reset != token.token
  }

  def "when i try and reset an expired token for someone that doesn't exist, i get no result"() {
    when: 'i try and reset the token'
      def reset = auth.resetExpiredRegistrationToken('fred@choppa-chip.com')
    then:
      reset == null
  }

}
