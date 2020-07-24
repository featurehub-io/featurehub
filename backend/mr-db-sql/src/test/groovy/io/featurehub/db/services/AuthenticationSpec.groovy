package io.featurehub.db.services

import io.ebean.DB
import io.ebean.Database
import io.featurehub.db.api.Opts
import io.featurehub.db.model.DbPerson
import io.featurehub.db.model.query.QDbPerson
import io.featurehub.db.publish.CacheSource
import io.featurehub.mr.model.Application
import io.featurehub.mr.model.ApplicationGroupRole
import io.featurehub.mr.model.ApplicationRoleType
import io.featurehub.mr.model.Group
import io.featurehub.mr.model.Person
import io.featurehub.mr.model.Portfolio
import spock.lang.Shared
import spock.lang.Specification

class AuthenticationSpec extends BaseSpec {
  @Shared AuthenticationSqlApi auth
  @Shared PersonSqlApi personApi
  @Shared ApplicationSqlApi appApi
  @Shared PortfolioSqlApi portfolioApi


  def setupSpec() {
    baseSetupSpec()
    auth = new AuthenticationSqlApi(database, convertUtils)
    personApi = new PersonSqlApi(database, convertUtils, archiveStrategy)
    portfolioApi = new PortfolioSqlApi(database, convertUtils, archiveStrategy)
    appApi = new ApplicationSqlApi(database, convertUtils, Mock(CacheSource), archiveStrategy)
  }

  def "I should be able to register a new user"() {
    when: "i register"
      personApi.create('william@featurehub.io', "William", null)
      Person person = auth.register("william", "william@featurehub.io", "yacht")
    then:
      person.id
      person.name == "william"
      person.email == "william@featurehub.io"
      !person.passwordRequiresReset
  }

  def "I cannot register twice"() {
    when: "i register"
      personApi.create('william-double-reg@featurehub.io', "William", null)
      Person person = auth.register("william", "william-double-reg@featurehub.io", "yacht")
    and: "i try again"
      Person person2 = auth.register("william", "william-double-reg@featurehub.io", "yacht")
    then:
      person != null
      person2 == null
  }

  def "I should be able to login"() {
    given: "i register"
      personApi.create('william1@featurehub.io', "William", null)
      Person person = auth.register("william", "william1@featurehub.io", "bathroom")
    when: "i login"
      Person loginPerson = auth.login("william1@featurehub.io", "bathroom")
    then:
      loginPerson
  }

  def "I should be able to login and change my password"() {
    given: "i register"
      personApi.create('william4@featurehub.io', "William", null)
      Person person = auth.register("william", "william4@featurehub.io", "bathroom")
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
      Person person = auth.register("william", "william-empty-pw@featurehub.io", "")
    then:
      person == null
  }

  def "i cannot change my password to an empty password"() {
    given: "i register"
      personApi.create('william-emptypw-change@featurehub.io', "William", null)
      Person person = auth.register("william", "william-emptypw-change@featurehub.io", "bathroom")
    when: "i change my password using the wrong one"
      Person changedPerson = auth.changePassword(person.id.id, "bathroom", "")
    then:
      changedPerson == null
  }

  def "i shouldn't be able to change my password if the old one is wrong"() {
    given: "i register"
      personApi.create('william-badpw@featurehub.io', "William", null)
      Person person = auth.register("william", "william-badpw@featurehub.io", "bathroom")
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
      Person person = auth.register("william2", "william2@featurehub.io", "yacht")
    when: "the super user changes my password"
      Person resetPerson = auth.resetPassword(person.id.id, "honey", superuser.toString(), false)
    then:
      resetPerson
      resetPerson.email == 'william2@featurehub.io'
  }

  def "a newly registered user has forgotten their password"() {
    given: "i register"
      personApi.create('william3@featurehub.io', "William", null)
      Person person = auth.register("william", "william3@featurehub.io", "bathroom")
    and: "i login"
      Person loginPerson = auth.login("william3@featurehub.io", "bathroom")
    when: "the super user changes my password"
      auth.resetPassword(person.id.id, "honey", superuser.toString(), false)
    and: "i try and login "
      Person newPerson = auth.login("william3@featurehub.io", "honey")
    then: "my password requires resetting"
      newPerson.passwordRequiresReset
  }

  def "a superuser changes my password, i reset it and i can't reset it again"() {
    given: "i register"
      personApi.create('william-temp@featurehub.io', "William", null)
      Person person = auth.register("william", "william-temp@featurehub.io", "bathroom")
    and: "the super user changes my password"
      auth.resetPassword(person.id.id, "honey", superuser.toString(), false)
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
      personApi.create('william-reset@featurehub.io', "William", null)
      Person person = auth.register("william", "william-reset@featurehub.io", "bathroom")
    when: "i try and reset my own password"
      Person resetUser = auth.resetPassword("william-reset@featurehub.io", "honey", person.id.id, false)
    then: "the system prevents me"
      !resetUser
  }

  def "i cannot reset my password to an empty password"() {
    given: "i register"
      personApi.create('william-reset1@featurehub.io',"William", null)
      Person person = auth.register("william", "william-reset1@featurehub.io", "bathroom")
    when: "i try and reset my password to empty"
      Person resetUser = auth.resetPassword(person.id.id, "", superuser.toString(), false)
    then: "the system prevents me"
      !resetUser
  }

  def "i can reset another user's password"() {
    given: "i register"
      personApi.create('william-reset2@featurehub.io', "William",superuser.toString())
      personApi.create('william-reset3@featurehub.io', "William",null)
      Person p2 = auth.register("william", "william-reset2@featurehub.io", "bathroom")
      Person p3 = auth.register("william", "william-reset3@featurehub.io", "bathroom")
    when: "i try and reset their password"
      Person resetUser = auth.resetPassword(p3.id.id, "bath2", superuser.toString(), false)
      Person resetUser2 = auth.resetPassword(p2.id.id, "bath2", superuser.toString(), false)
    then:
      resetUser
  }

  def "A user who is a portfolio manager gets their application roles when they login"() {
    given: "i register"
      personApi.create('portman26@mailinator.com', "Portman26",superuser.toString())
      Person p2 = auth.register("william", "portman26@mailinator.com", "hooray")
    and: "i create a new portfolio"
      Portfolio portfolio1 = portfolioApi.createPortfolio(new Portfolio().name("persontestportfolio").organizationId(org.getId()), Opts.empty(), superPerson)
    and: "i create an application in that portfolio"
      def app1 = appApi.createApplication(portfolio1.id, new Application().name("persontest-app1").description("some desc"), superPerson)
    and: "i make the user a portfolio manager"
      def portfolioGroup = groupSqlApi.createPortfolioGroup(portfolio1.id,
        new Group().name("admin-group").admin(true)
          .applicationRoles([new ApplicationGroupRole().applicationId(app1.id)
                               .roles([ApplicationRoleType.FEATURE_EDIT])]), superPerson)
      portfolioGroup = groupSqlApi.updateGroup(portfolioGroup.id, portfolioGroup.members([p2]),
        true, false, false, Opts.empty())
    when: "i login"
      def user = auth.login(p2.email, "hooray")
    then: "i have the application role permission to the portfolio"
      user.groups.find({it.applicationRoles.find({ar -> ar.roles.contains(ApplicationRoleType.FEATURE_EDIT) && ar.applicationId == app1.id})})
  }
}
