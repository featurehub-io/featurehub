package io.featurehub.db.services


import io.featurehub.db.api.Opts
import io.featurehub.db.api.UserStateApi
import io.featurehub.db.model.DbOrganization
import io.featurehub.db.model.DbPortfolio
import io.featurehub.encryption.WebhookEncryptionService
import io.featurehub.mr.events.common.CacheSource
import io.featurehub.mr.model.Application
import io.featurehub.mr.model.CreateApplication
import io.featurehub.mr.model.CreateEnvironment
import io.featurehub.mr.model.CreateGroup
import io.featurehub.mr.model.Environment
import io.featurehub.mr.model.Group
import io.featurehub.mr.model.HiddenEnvironments
import spock.lang.Shared

class UserStateSpec extends BaseSpec {
  @Shared PersonSqlApi personSqlApi
  @Shared DbPortfolio portfolio1
  @Shared ApplicationSqlApi appApi
  @Shared EnvironmentSqlApi envApi
  @Shared Application app1
  @Shared Group groupInPortfolio1
  @Shared Environment env1
  @Shared UserStateApi userStateApi

  def setupSpec() {
    baseSetupSpec()
    personSqlApi = new PersonSqlApi(database, convertUtils, archiveStrategy, Mock(InternalGroupSqlApi))

    appApi = new ApplicationSqlApi(convertUtils, Mock(CacheSource), archiveStrategy, Mock(InternalFeatureApi))
    envApi = new EnvironmentSqlApi(database, convertUtils, Mock(CacheSource), archiveStrategy, internalFeatureApi, Mock(WebhookEncryptionService))
    userStateApi = new UserStateSqlApi(convertUtils)

    // now set up the environments we need
    DbOrganization organization = Finder.findDbOrganization()
    portfolio1 = new DbPortfolio.Builder().name("port-user-spec").description("x").whoCreated(dbSuperPerson).organization(organization).build()
    database.save(portfolio1)

    // create the portfolio group
    groupInPortfolio1 = groupSqlApi.createGroup(portfolio1.id, new CreateGroup().name("p1-user-spec-admin").admin(true), superPerson)
    groupSqlApi.addPersonToGroup(groupInPortfolio1.id, superPerson.id.id, Opts.empty())

    app1 = appApi.createApplication(portfolio1.id, new CreateApplication().description("x").name('app1-user-spec'), superPerson)
    assert app1 != null && app1.id != null

    env1 = envApi.create(new CreateEnvironment().name("dev").description("desc"), app1.id, superPerson)
  }

  def "Basic CRUD on HiddenEnvironments works as expected"() {
    given: "I create a config for a person"
      userStateApi.saveHiddenEnvironments(superPerson, new HiddenEnvironments().addEnvironmentIdsItem(env1.id), app1.id)
    when: "I get it"
      def he = userStateApi.getHiddenEnvironments(superPerson, app1.id)
    and: "save an empty one"
      userStateApi.saveHiddenEnvironments(superPerson, new HiddenEnvironments(), app1.id)
    then:
      userStateApi.getHiddenEnvironments(superPerson, app1.id) == null
      he != null
      he.environmentIds.size() == 1
      he.environmentIds[0] == env1.id
  }

  def "I attempt to add > max environments and i get an invalid state"() {
    given: "a new user state"
        def us = new UserStateSqlApi(convertUtils)
    and: "a new user state with too manny environments"
      def he = new HiddenEnvironments();
      for(int count = 0; count < us.maximumEnvironmentsPerApplication + 5; count ++) {
        he.addEnvironmentIdsItem(UUID.randomUUID())
      }
    when: "i save"
        us.saveHiddenEnvironments(superPerson, he, app1.id)
    then:
        thrown UserStateApi.InvalidUserStateException
  }

  def "I attempt to add environments that aren't valid uuids"() {
    given: "a new user state impl"
      def us = new UserStateSqlApi(convertUtils)
    and: "a new user state with invalid environment uuids"
      def he = new HiddenEnvironments().addEnvironmentIdsItem(UUID.randomUUID()).addEnvironmentIdsItem(UUID.randomUUID())
    when: "i save"
        us.saveHiddenEnvironments(superPerson, he, app1.id)
    then:
        thrown UserStateApi.InvalidUserStateException
  }

  def "I attempt to add environments that don't exist"() {
    given: "a new user state impl"
      def us = new UserStateSqlApi(convertUtils)
    and: "a new user state with invalid environment uuids"
      def he = new HiddenEnvironments().addEnvironmentIdsItem(UUID.randomUUID()).addEnvironmentIdsItem(UUID.randomUUID())
    when: "i save"
        us.saveHiddenEnvironments(superPerson, he, app1.id)
    then:
        thrown UserStateApi.InvalidUserStateException
  }
}
