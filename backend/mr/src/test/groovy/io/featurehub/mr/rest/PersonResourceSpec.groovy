package io.featurehub.mr.rest

import io.featurehub.db.api.GroupApi
import io.featurehub.db.api.Opts
import io.featurehub.db.api.PersonApi
import io.featurehub.mr.api.PersonServiceDelegate
import io.featurehub.mr.auth.AuthManagerService
import io.featurehub.mr.model.CreatePersonDetails
import io.featurehub.mr.model.Person
import io.featurehub.mr.model.PersonId
import io.featurehub.mr.model.RegistrationUrl
import io.featurehub.mr.model.UpdatePerson
import io.featurehub.mr.resources.PersonResource
import jakarta.ws.rs.BadRequestException
import jakarta.ws.rs.ForbiddenException
import jakarta.ws.rs.NotFoundException
import jakarta.ws.rs.core.SecurityContext
import spock.lang.Specification

// Irina was supposed to write these

class PersonResourceSpec extends Specification {
  GroupApi groupApi
  PersonApi personApi
  AuthManagerService authManager
  PersonResource resource
  SecurityContext ctx

  def setup() {
    groupApi = Mock(GroupApi)
    personApi = Mock(PersonApi)
    authManager = Mock(AuthManagerService)
    ctx = Mock(SecurityContext)
    authManager.from(ctx) >> new Person().id(new PersonId().id(UUID.randomUUID()))

    System.setProperty("register.url", "%s")
    resource = new PersonResource(personApi, groupApi, authManager)
  }

  def "i can only create a person if i am an admin"() {
    given: "i am not an admin"
      authManager.isAnyAdmin(_) >> false
    when: "i ask to create a person"
      resource.createPerson(new CreatePersonDetails(), new PersonServiceDelegate.CreatePersonHolder(), ctx)
    then:
      thrown ForbiddenException
  }

  def "failing to create a person throws a bad request"() {
    given: "i have a new person"
      CreatePersonDetails cpd = new CreatePersonDetails().email("torvill@f.com")
    and: "i am an admin"
      authManager.isAnyAdmin(_) >> true
    when: "i ask to create a new person"
      resource.createPerson(cpd, new PersonServiceDelegate.CreatePersonHolder(), ctx)
    then:
      thrown(BadRequestException)
  }

  def "can create a person who has no groups to add"() {
    given: "i have a new person"
      CreatePersonDetails cpd = new CreatePersonDetails().email("torvill@f.com").name("name")
      PersonApi.PersonToken token = new PersonApi.PersonToken("fred", UUID.randomUUID())
      personApi.create(cpd.getEmail(), "name", _) >> token
    and: "i am an admin"
      authManager.isAnyAdmin(_) >> true
    when: "i ask to create a new person"
      RegistrationUrl url = resource.createPerson(cpd, new PersonServiceDelegate.CreatePersonHolder(), ctx)
    then:
      url.registrationUrl == "fred"
  }

  def "a person who has groups to add will tell groupApi to add them"() {
    given: "i have a new person"
      CreatePersonDetails cpd = new CreatePersonDetails().email("torvill@f.com").name("name")
      cpd.addGroupIdsItem(UUID.randomUUID())
      cpd.addGroupIdsItem(UUID.randomUUID())
    and: "i am an admin"
      authManager.isAnyAdmin(_) >> true
    and: "i have setup a create response"
      PersonApi.PersonToken token = new PersonApi.PersonToken("fred", UUID.randomUUID())
      personApi.create(cpd.getEmail(), "name", _) >> token
    when: "i ask to create a new person"
      RegistrationUrl url = resource.createPerson(cpd, new PersonServiceDelegate.CreatePersonHolder(), ctx)
    then:
      url.registrationUrl == "fred"
      1 * groupApi.addPersonToGroup(cpd.groupIds.get(0), token.id, (Opts)_)
      1 * groupApi.addPersonToGroup(cpd.groupIds.get(1), token.id, (Opts)_)
  }

  def "a person who is not an admin cannot search"() {
    given: "i am not an admin"
      authManager.isAnyAdmin(_) >> false
    when: "i search for a person"
      resource.findPeople(new PersonServiceDelegate.FindPeopleHolder(), ctx)
    then:
      thrown ForbiddenException
  }

  def "a person who is not an admin cannot get a person"() {
    given: "i am not an admin"
      authManager.isAnyAdmin(_) >> false
    when: "i search for a person"
      resource.getPerson("x", new PersonServiceDelegate.GetPersonHolder(), ctx)
    then:
      thrown ForbiddenException
  }



  def "a person who updates must be an admin"() {
    when: "I try and update a person without being an admin"
      resource.updatePerson(UUID.randomUUID(), new Person(), new PersonServiceDelegate.UpdatePersonHolder(), ctx)
    then:
      thrown ForbiddenException
  }

  def "an administrator is allowed to update a person"() {
    given: "I have an admin security token"
      authManager.isAnyAdmin(_) >> true
    and:
      personApi.update(_, _, _, _) >> new Person()
    when: "I try and update a person and am an admin"
      def person = resource.updatePerson(UUID.randomUUID(), new Person(), new PersonServiceDelegate.UpdatePersonHolder(), ctx)
    then:
      person != null
  }

  def "an administrator cannot update a non-existent person"() {
    given: "I have an admin security token"
      authManager.isAnyAdmin(_) >> true
    and: "no such person exists"
      personApi.update(_, _, _, _) >> null
    when: "I try and update a person and am an admin"
      def person = resource.updatePerson(UUID.randomUUID(),
        new Person().id(new PersonId().id(UUID.randomUUID())),
        new PersonServiceDelegate.UpdatePersonHolder(), ctx)
    then:
      thrown NotFoundException
  }

  def "updateV2: a person who updates must be an admin"() {
    when: "I try and update a person without being an admin"
      resource.updatePersonV2(UUID.randomUUID(), new UpdatePerson(), ctx)
    then:
      thrown ForbiddenException
  }

  def "updateV2: an administrator is allowed to update a person"() {
    given: "we have a person id"
      def pid = UUID.randomUUID()
      personApi.updateV2(_, _, _) >> pid
      authManager.isAnyAdmin((Person)_) >> true
    when: "I try and update a person and am an admin"
      resource.updatePersonV2(pid, new UpdatePerson(), ctx)
    then:
      1 == 1
  }

  def "updateV2: an administrator cannot update a non-existent person"() {
    given: "I have an admin security token"
      authManager.isAnyAdmin(_) >> true
    and: "no such person exists"
      personApi.updateV2(_, _, _) >> null
    when: "I try and update a person and am an admin"
      def person = resource.updatePersonV2(UUID.randomUUID(),
        new UpdatePerson(), ctx)
    then:
      thrown NotFoundException
  }
}
