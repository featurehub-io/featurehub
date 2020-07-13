package io.featurehub.db.services

import io.ebean.DB
import io.ebean.Database
import io.featurehub.db.model.DbPerson
import io.featurehub.db.model.query.QDbPerson
import io.featurehub.db.publish.CacheSource
import io.featurehub.mr.model.Person
import spock.lang.Shared
import spock.lang.Specification

class AuthenticationSpec extends BaseSpec {
  @Shared AuthenticationSqlApi auth
  @Shared PersonSqlApi personApi


  def setupSpec() {
    baseSetupSpec()
    auth = new AuthenticationSqlApi(database, convertUtils)
    personApi = new PersonSqlApi(database, convertUtils, archiveStrategy)
  }

  def "I should be able to register a new user"() {
    when: "i register"
      personApi.create('rob@featurehub.io', null)
      Person person = auth.register("rob", "rob@featurehub.io", "yacht")
    then:
      person.id
      person.name == "rob"
      person.email == "rob@featurehub.io"
      !person.passwordRequiresReset
  }

  def "I cannot register twice"() {
    when: "i register"
      personApi.create('rob-double-reg@featurehub.io', null)
      Person person = auth.register("rob", "rob-double-reg@featurehub.io", "yacht")
    and: "i try again"
      Person person2 = auth.register("rob", "rob-double-reg@featurehub.io", "yacht")
    then:
      person != null
      person2 == null
  }

  def "I should be able to login"() {
    given: "i register"
      personApi.create('rob1@featurehub.io', null)
      Person person = auth.register("rob", "rob1@featurehub.io", "bathroom")
    when: "i login"
      Person loginPerson = auth.login("rob1@featurehub.io", "bathroom")
    then:
      loginPerson
  }

  def "I should be able to login and change my password"() {
    given: "i register"
      personApi.create('rob4@featurehub.io', null)
      Person person = auth.register("rob", "rob4@featurehub.io", "bathroom")
    and: "i login"
      Person loginPerson = auth.login("rob4@featurehub.io", "bathroom")
    when: "i change my password"
      auth.changePassword(loginPerson.id.id, "bathroom", "honey")
    and: "login"
      Person loginPerson2 = auth.login("rob4@featurehub.io", "honey")
    then: "i still can"
      loginPerson2
      loginPerson2.id == loginPerson.id
  }

  def "i cannot register with an empty password"() {
    when:
      personApi.create('rob-empty-pw@featurehub.io', null)
      Person person = auth.register("rob", "rob-empty-pw@featurehub.io", "")
    then:
      person == null
  }

  def "i cannot change my password to an empty password"() {
    given: "i register"
      personApi.create('rob-emptypw-change@featurehub.io', null)
      Person person = auth.register("rob", "rob-emptypw-change@featurehub.io", "bathroom")
    when: "i change my password using the wrong one"
      Person changedPerson = auth.changePassword(person.id.id, "bathroom", "")
    then:
      changedPerson == null
  }

  def "i shouldn't be able to change my password if the old one is wrong"() {
    given: "i register"
      personApi.create('rob-badpw@featurehub.io', null)
      Person person = auth.register("rob", "rob-badpw@featurehub.io", "bathroom")
    and: "i login"
      Person loginPerson = auth.login("rob-badpw@featurehub.io", "bathroom")
    when: "i change my password using the wrong one"
      Person changedPerson = auth.changePassword(loginPerson.id.id, "yacht", "honey")
    and: "i try and login"
      Person loginPerson2 = auth.login("rob-badpw@featurehub.io", "honey")
    then:
      !loginPerson2
      !changedPerson
  }

  def "a superuser can change a person's password"() {
    given: "i register"
      personApi.create('rob2@featurehub.io', null)
      Person person = auth.register("rob2", "rob2@featurehub.io", "yacht")
    when: "the super user changes my password"
      Person resetPerson = auth.resetPassword(person.id.id, "honey", superuser.toString(), false)
    then:
      resetPerson
      resetPerson.email == 'rob2@featurehub.io'
  }

  def "a newly registered user has forgotten their password"() {
    given: "i register"
      personApi.create('rob3@featurehub.io', null)
      Person person = auth.register("rob", "rob3@featurehub.io", "bathroom")
    and: "i login"
      Person loginPerson = auth.login("rob3@featurehub.io", "bathroom")
    when: "the super user changes my password"
      auth.resetPassword(person.id.id, "honey", superuser.toString(), false)
    and: "i try and login "
      Person newPerson = auth.login("rob3@featurehub.io", "honey")
    then: "my password requires resetting"
      newPerson.passwordRequiresReset
  }

  def "a superuser changes my password, i reset it and i can't reset it again"() {
    given: "i register"
      personApi.create('rob-temp@featurehub.io', null)
      Person person = auth.register("rob", "rob-temp@featurehub.io", "bathroom")
    and: "the super user changes my password"
      auth.resetPassword(person.id.id, "honey", superuser.toString(), false)
    and: "i reset my password"
      auth.replaceTemporaryPassword(person.id.id, "buoy")
    and: "i try and reset it again"
      Person reset = auth.replaceTemporaryPassword(person.id.id, "buoy")
    when: "i login"
      Person newPerson = auth.login("rob-temp@featurehub.io", "buoy")
    then: "my password does not require reset"
      !newPerson.passwordRequiresReset
      reset == null
  }

  def "a person cannot reset their own password"() {
    given: "i register"
      personApi.create('rob-reset@featurehub.io', null)
      Person person = auth.register("rob", "rob-reset@featurehub.io", "bathroom")
    when: "i try and reset my own password"
      Person resetUser = auth.resetPassword("rob-reset@featurehub.io", "honey", person.id.id, false)
    then: "the system prevents me"
      !resetUser
  }

  def "i cannot reset my password to an empty password"() {
    given: "i register"
      personApi.create('rob-reset1@featurehub.io', null)
      Person person = auth.register("rob", "rob-reset1@featurehub.io", "bathroom")
    when: "i try and reset my password to empty"
      Person resetUser = auth.resetPassword(person.id.id, "", superuser.toString(), false)
    then: "the system prevents me"
      !resetUser
  }

  def "i can reset another user's password"() {
    given: "i register"
      personApi.create('rob-reset2@featurehub.io', superuser.toString())
      personApi.create('rob-reset3@featurehub.io', null)
      Person p2 = auth.register("rob", "rob-reset2@featurehub.io", "bathroom")
      Person p3 = auth.register("rob", "rob-reset3@featurehub.io", "bathroom")
    when: "i try and reset their password"
      Person resetUser = auth.resetPassword(p3.id.id, "bath2", superuser.toString(), false)
      Person resetUser2 = auth.resetPassword(p2.id.id, "bath2", superuser.toString(), false)
    then:
      resetUser
  }
}
