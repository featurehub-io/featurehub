package io.featurehub.mr.resources.oauth2

import io.featurehub.db.api.AuthenticationApi
import io.featurehub.db.api.GroupApi
import io.featurehub.db.api.OrganizationApi
import io.featurehub.db.api.PersonApi
import io.featurehub.db.api.PortfolioApi
import io.featurehub.mr.auth.AuthenticationRepository
import io.featurehub.mr.model.Person
import io.featurehub.mr.model.PersonId
import io.featurehub.mr.model.UpdatePerson
import io.featurehub.mr.utils.PortfolioUtils
import org.apache.commons.lang3.RandomStringUtils
import spock.lang.Specification

class OAuth2MRAdapterSpec extends Specification {
  PersonApi personApi
  AuthenticationApi authenticationApi
  PortfolioApi portfolioApi
  GroupApi groupApi
  AuthenticationRepository authenticationRepository
  PortfolioUtils portfolioUtils
  OrganizationApi orgApi
  OAuth2MRAdapter adapter
  Person person
  String email
  String uname
  String apiToken

  def setup() {
    personApi = Mock()
    authenticationRepository = Mock()
    authenticationApi = Mock()
    portfolioUtils = Mock()
    portfolioApi = Mock()
    groupApi = Mock()
    orgApi = Mock()

    adapter = new OAuth2MRAdapter(personApi, authenticationApi, portfolioApi, groupApi, authenticationRepository, portfolioUtils, orgApi)

    email = RandomStringUtils.randomAlphabetic(10) + "@example.com"
    uname = RandomStringUtils.randomAlphabetic(20)
    person = new Person().id(new PersonId(id: UUID.randomUUID()))
    apiToken = "1234"
  }

  def "a user tries to login but the creation-first policy is on and they don't exist"() {
    when:
      def response = adapter.successfulCompletion(email, "me", true, "http://failure", "http://success", "provider")
    then: "we check if we have the person"
      1 * personApi.get(email, _) >> null
    and: "no other mocks called"
      0 * _
    and:
      response.status == 302
      response.location?.toString() == "http://failure"
  }

  def "a login creates a new user by default"() {
    when:
      def response = adapter.successfulCompletion(email, "me", false, "http://failure", "http://success", "provider")
    then: "we check if we have the person"
      1 * personApi.get(email, _) >> null
    and: "we aren't the 1st person"
      1 * personApi.noUsersExist() >> false
    and: "creating their person object is ok"
      1 * personApi.create(email, "me", null)
    and: "they can register the person"
      1 * authenticationApi.register("me", email, null, null) >> person
    and: "we stash their session"
      1 * authenticationRepository.put(person) >> apiToken
    and: "no other mocks called"
      0 * _
    and: "the result is as expected"
      response.getHeaderString("location") == "http://success"
      response.status == 302
      response.getCookies()["bearer-token"] != null
  }

  def "a user who already exists logs in"() {
    given:
      person.version(15)
    when:
      def response = adapter.successfulCompletion(email, uname, false, "http://failure", "http://success", "provider")
    then: "we check if we have the person"
      1 * personApi.get(email, _) >> person
    and: "it tries to update the user's name"
      1 * personApi.updateV2(person.id.id, { UpdatePerson up ->
        up.version == person.version
        up.name == uname
      }, person.id.id)
    and: "it acks we have authenticated"
      1 * authenticationApi.updateLastAuthenticated(person.id.id)
    and: "we stash their session"
      1 * authenticationRepository.put(person) >> apiToken
    and: "no other mocks called"
      0 * _
    and: "the result is as expected"
      response.getHeaderString("location") == "http://success"
      response.status == 302
      response.getCookies()["bearer-token"] != null
  }
}
