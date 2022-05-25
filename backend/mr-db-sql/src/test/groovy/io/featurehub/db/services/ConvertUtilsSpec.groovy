package io.featurehub.db.services

import io.featurehub.db.api.FillOpts
import io.featurehub.db.api.Opts
import io.featurehub.db.model.DbLogin
import io.featurehub.db.model.DbPerson
import io.featurehub.mr.model.PersonType
import io.featurehub.mr.model.SortOrder

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

class ConvertUtilsSpec extends Base2Spec {
  PersonSqlApi personSqlApi
  AuthenticationSqlApi authenticationSqlApi

  def setup() {
    personSqlApi = new PersonSqlApi(db, convertUtils, archiveStrategy, Mock(InternalGroupSqlApi))
    authenticationSqlApi = new AuthenticationSqlApi(db, convertUtils)
  }

  def "a new person who has never authenticated will not appear to have whenLastAuthenticated set"() {
    given: "i have a new person"
      def email = 'new-person0@me.com'
      def person = new DbPerson.Builder().email(email).name(email).build()
      db.save(new DbPerson.Builder().email(email).name(email).build())
      db.commitTransaction()  // have to do this otherwise we can't find them
    when: "i find the person"
      def people = personSqlApi.search(email, SortOrder.ASC, 0, 0, Set.of(PersonType.PERSON), Opts.opts(FillOpts.PersonLastLoggedIn))
    then:
      people.people.size() == 1
      people.people[0].whenLastAuthenticated == null
      people.people[0].whenLastSeen == null
  }

  def "a person who has logged in will appear to have whenLastAuthenticatedSet"() {
    given: "i have a new person"
      def email = 'new-person1@me.com'
      def person = new DbPerson.Builder().email(email).name(email).build()
      db.save(person)
      db.commitTransaction()
      def whenLastAuthenticated = Instant.now()
      authenticationSqlApi.updateLastAuthenticated(person, whenLastAuthenticated)
    when: "i find the person"
      def people = personSqlApi.search(email, SortOrder.ASC, 0, 0, Set.of(PersonType.PERSON), Opts.opts(FillOpts.PersonLastLoggedIn))
    and: "i find without the logged in option"
      def peopleClean = personSqlApi.search(email, SortOrder.ASC, 0, 0, Set.of(PersonType.PERSON), Opts.empty())
    then:
      people.people.size() == 1
      people.people[0].whenLastAuthenticated == whenLastAuthenticated.atOffset(ZoneOffset.UTC)
      people.people[0].whenLastSeen == null
      peopleClean.people.size() == 1
      peopleClean.people[0].whenLastAuthenticated == null
      peopleClean.people[0].whenLastSeen == null
  }

  def "a person who has a dblogin will have a last seen"() {
    given: "i have a new person"
      def email = 'new-person2@me.com'
      def person = new DbPerson.Builder().email(email).name(email).build()
      def whenLastAuthenticated = Instant.now()
      person.whenLastAuthenticated = whenLastAuthenticated
      db.save(person)
    and: "they have a login"
      def whenLastSeen = whenLastAuthenticated.plusMillis(2000)
      def login = new DbLogin.Builder().person(person).lastSeen(whenLastSeen).token("xxxx").build()
      db.save(login)
    and: "we commit the transaction"
      db.commitTransaction()
    when: "i find the person"
      def people = personSqlApi.search(email, SortOrder.ASC, 0, 0, Set.of(PersonType.PERSON), Opts.opts(FillOpts.PersonLastLoggedIn))
    and: "i find them again without the fill opts"
      def peopleClean = personSqlApi.search(email, SortOrder.ASC, 0, 0, Set.of(PersonType.PERSON), Opts.empty())
    then:
      people.people.size() == 1
      people.people[0].whenLastAuthenticated == whenLastAuthenticated.atOffset(ZoneOffset.UTC)
      people.people[0].whenLastSeen == whenLastSeen.atOffset(ZoneOffset.UTC)
      peopleClean.people.size() == 1
      peopleClean.people[0].whenLastAuthenticated == null
      peopleClean.people[0].whenLastSeen == null
  }
}
