package io.featurehub.db.services

import groovy.transform.CompileStatic
import io.featurehub.db.api.EnvironmentApi
import io.featurehub.db.api.FillOpts
import io.featurehub.db.api.GroupApi
import io.featurehub.db.api.Opts
import io.featurehub.db.model.DbEnvironment
import io.featurehub.db.model.DbOrganization
import io.featurehub.db.model.DbPerson
import io.featurehub.db.model.DbPortfolio
import io.featurehub.db.model.query.QDbEnvironment
import io.featurehub.mr.events.common.CacheSource
import io.featurehub.mr.model.Application
import io.featurehub.mr.model.ApplicationRoleType
import io.featurehub.mr.model.Environment
import io.featurehub.mr.model.EnvironmentGroupRole
import io.featurehub.mr.model.Group
import io.featurehub.mr.model.RoleType
import io.featurehub.mr.model.SortOrder
import io.featurehub.mr.model.UpdateEnvironment
import org.apache.commons.lang3.RandomStringUtils
import org.jetbrains.annotations.Nullable

class Environment2Spec extends Base2Spec {
  PersonSqlApi personSqlApi
  DbPortfolio portfolio1
  DbPortfolio portfolio2
  ApplicationSqlApi appApi
  EnvironmentSqlApi envApi
  Application app1
  Application app2
  Application appTreeEnvs
  Group groupInPortfolio1
  CacheSource cacheSource

  def setup() {
    db.commitTransaction()
    personSqlApi = new PersonSqlApi(db, convertUtils, archiveStrategy, Mock(InternalGroupSqlApi))
    cacheSource = Mock(CacheSource)

    appApi = new ApplicationSqlApi(db, convertUtils, cacheSource, archiveStrategy)
    envApi = new EnvironmentSqlApi(db, convertUtils, cacheSource, archiveStrategy)

    // now set up the environments we need
    DbOrganization organization = Finder.findDbOrganization()
    portfolio1 = new DbPortfolio.Builder().name(RandomStringUtils.randomAlphabetic(8) + "p1-app-1-env1").whoCreated(dbSuperPerson).organization(organization).build()
    db.save(portfolio1)
    portfolio2 = new DbPortfolio.Builder().name(RandomStringUtils.randomAlphabetic(8) + "p1-app-2-env1").whoCreated(dbSuperPerson).organization(organization).build()
    db.save(portfolio2)

    // create the portfolio group
    groupInPortfolio1 = groupSqlApi.createGroup(portfolio1.id, new Group().name("p1-app-1-env1-portfolio-group").admin(true), superPerson)
    groupSqlApi.addPersonToGroup(groupInPortfolio1.id, superPerson.id.id, Opts.empty())

    app1 = appApi.createApplication(portfolio1.id, new Application().name('app-1-env'), superPerson)
    assert app1 != null && app1.id != null
    app2 = appApi.createApplication(portfolio2.id, new Application().name('app-2-env'), superPerson)
    assert app2 != null
    appTreeEnvs = appApi.createApplication(portfolio2.id, new Application().name('app-tree-env'), superPerson)
    assert appTreeEnvs != null
    if (db.currentTransaction() != null && db.currentTransaction().active) {
      db.commitTransaction()
    }
  }

  def "i can create, find and then update an existing environment"() {
    when: "i create a new environment"
      Environment e = envApi.create(new Environment().name("env-1-app1").description("desc app 1 env 1"), app1, superPerson)
      List<Environment> createSearch = envApi.search(app1.id, e.name, SortOrder.ASC, Opts.empty(), superPerson)
      Environment eGet = envApi.get(e.id, Opts.empty(), superPerson)
    and:
      def originalEnv = envApi.get(e.id, Opts.empty(), superPerson)
      Environment u = envApi.update(e.id, originalEnv.name("env-1-app-1 update").description("new desc"), Opts.empty())
      List<Environment> updateSearch = envApi.search(app1.id, u.name, SortOrder.ASC, Opts.empty(), superPerson)
      Environment uGet = envApi.get(e.id, Opts.empty(), superPerson)
    and:
      boolean success = envApi.delete(u.id)
      List<Environment> deleteSearch = envApi.search(app1.id, u.name, SortOrder.ASC, Opts.empty(), superPerson)
    then:
      e != null
      createSearch.size() == 1
      createSearch[0].name == e.name
      eGet != null
      eGet.name == e.name
      eGet.description == e.description
      u != null
      updateSearch.size() == 1
      updateSearch[0].name == u.name
      uGet != null
      uGet.name == u.name
      uGet.description == u.description
      success
      deleteSearch.size() == 0
  }

  def "i cannot create environments with duplicate names in the same application"() {
    when: "i create a new environment"
      Environment e = envApi.create(new Environment().name("env-1-app1-dupe1").description("desc app 1 env 1"), app1, superPerson)
    and: "i create another with the same name"
      envApi.create(new Environment().name("env-1-app1-dupe1").description("desc app 1 env 1"), app1, superPerson)
    then:
      thrown EnvironmentApi.DuplicateEnvironmentException
  }

  def "I can create environments with the same name in different applications"() {
    when: "i create a new environment"
      Environment e = envApi.create(new Environment().name("env-1-app1-dupe3").description("desc app 1 env 1"), app1, superPerson)
    and: "i create another with the same name"
      Environment e2 = envApi.create(new Environment().name("env-1-app1-dupe3").description("desc app 1 env 1"), app2, superPerson)
    then:
      envApi.get(e.id, Opts.empty(), superPerson) != null
      envApi.get(e2.id, Opts.empty(), superPerson) != null
  }

  @CompileStatic
  @Nullable DbEnvironment environment(UUID id) {
    def env = new QDbEnvironment().id.eq(id).findOne()
    if (env != null) {
      env.refresh()
    }
    return env
  }

  def "I create two environments and they both get unpublished when I asked them to be"() {
    given: "I create two environments"
      def e1 = envApi.create(new Environment().name("env-1").description("env 1"), app1, superPerson)
      def e2 = envApi.create(new Environment().name("env-2").description("env 1"), app1, superPerson)
    when: "I unpublished them"
      envApi.unpublishEnvironments(app1.id,  null)
    then:
      1 * cacheSource.deleteEnvironment({ UUID id -> id == e1.id })
      1 * cacheSource.deleteEnvironment({ UUID id -> id == e2.id })
      environment(e1.id).whenUnpublished != null
      environment(e2.id).whenUnpublished != null
  }

  def "I create two environments and I unpublish only one of them"() {
    given: "I create two environments"
      def e1 = envApi.create(new Environment().name("env-1").description("env 1"), app1, superPerson)
      def e2 = envApi.create(new Environment().name("env-2").description("env 1"), app1, superPerson)
    when: "I unpublished them"
      envApi.unpublishEnvironments(app1.id,  [e1.id])
    then:
      1 * cacheSource.deleteEnvironment({ UUID id -> id == e1.id })
      0 * cacheSource.deleteEnvironment({ UUID id -> id == e2.id })
      environment(e1.id).whenUnpublished != null
      environment(e2.id).whenUnpublished == null
  }


  def "i cannot create two differently named environments and then update them to have the same name"() {
    when: "i create a new environment"
      Environment e = envApi.create(new Environment().name("env-1-app1-dupe1").description("desc app 1 env 1"), app1, superPerson)
    and: "i create another with the different name"
      Environment e2 = envApi.create(new Environment().name("env-1-app1-dupe2").description("desc app 1 env 1"), app1, superPerson)
    and: "then update it to be the same name"
      e2.name("env-1-app1-dupe1")
      envApi.update(e2.id, e2, Opts.empty())
    then:
      thrown EnvironmentApi.DuplicateEnvironmentException
  }

  def "i can create several environments and update them to move them around in the tree"() {
    given: "i create five new environments in a tree"
      List<Environment> envs = []
      envs.add(envApi.create(new Environment().name("env-1-prior-id").description("1"), appTreeEnvs, superPerson))
      envs.add(envApi.create(new Environment().name("env-2-prior-id").description("2").priorEnvironmentId(envs[0].id), appTreeEnvs, superPerson))
      envs.add(envApi.create(new Environment().name("env-3-prior-id").description("3").priorEnvironmentId(envs[1].id), appTreeEnvs, superPerson))
      envs.add(envApi.create(new Environment().name("env-4-prior-id").description("4").priorEnvironmentId(envs[2].id), appTreeEnvs, superPerson))  // envs[3]
      envs.add(envApi.create(new Environment().name("env-5-prior-id").description("5").priorEnvironmentId(envs[3].id), appTreeEnvs, superPerson))  // envs[4]
    and:
      def myApp = appApi.getApplication(appTreeEnvs.id, Opts.opts(FillOpts.Environments))
    when: "i change the order around"
      envs[3] = envApi.update(envs[3].id, envs[3].priorEnvironmentId(envs[4].id), Opts.empty())
      envs[4] = envApi.get(envs[4].id, Opts.empty(), superPerson)
    and:
      def myApp2 = appApi.getApplication(appTreeEnvs.id, Opts.opts(FillOpts.Environments))
    then:
      myApp.environments.find({it.name == 'env-3-prior-id'}).priorEnvironmentId == myApp.environments.find({it.name == 'env-2-prior-id'}).id
      myApp2.environments.find({it.name == 'env-4-prior-id'}).priorEnvironmentId == myApp.environments.find({it.name == 'env-5-prior-id'}).id
      myApp2.environments.find({it.name == 'env-5-prior-id'}).priorEnvironmentId == myApp.environments.find({it.name == 'env-3-prior-id'}).id
  }

  def "i can create two environments and cannot create a circular link on update"() {
    given: "i create five new environments in a tree"
      List<Environment> envs = []
      envs.add(envApi.create(new Environment().name("env-1-prior-7").description("1"), appTreeEnvs, superPerson))
      envs.add(envApi.create(new Environment().name("env-2-prior-7").description("2").priorEnvironmentId(envs[0].id), appTreeEnvs, superPerson))
    when: "i set the environments to point to each other"
      envs[0].priorEnvironmentId = envs[1].id
      envs[1].priorEnvironmentId = envs[0].id
      def result = envApi.setOrdering(appTreeEnvs, envs);
    then:
      result == null
  }

  def "if i have only one environment, ordering is ignored"() {
    given: "i create a single environment"
      List<Environment> envs = []
      envs.add(envApi.create(new Environment().name("env-single-prior-0").description("1"), appTreeEnvs, superPerson))
    when:
      def result = envApi.setOrdering(appTreeEnvs, envs)
    then:
      result == envs
  }

  def "if have two environments, with the 2nd pointing at 1st, and then delete 1st, 2nd should no longer point to 1st"() {
    given: "i create five new environments in a tree"
      List<Environment> envs = []
      def original = envApi.create(new Environment().name("env-1-prior-7").description("1"), appTreeEnvs, superPerson)
      envs.add(envApi.create(new Environment().name("env-2-prior-7").description("2").priorEnvironmentId(original.id), appTreeEnvs, superPerson))
    when: "i delete the original"
      envApi.delete(original.id)
    then:
      envApi.get(envs[0].id, Opts.empty(), superPerson).priorEnvironmentId == null
  }

  def "if i have 3 environments in order and delete the one in the middle, the last one now points to the 1st one"() {
    given: "i have 3 environments"
      List<Environment> envs = []
      envs.add(envApi.create(new Environment().name("env-1-prior-7").description("1"), appTreeEnvs, superPerson))
      envs.add(envApi.create(new Environment().name("env-2-prior-7").description("2").priorEnvironmentId(envs[0].id), appTreeEnvs, superPerson))
      envs.add(envApi.create(new Environment().name("env-3-prior-7").description("3").priorEnvironmentId(envs[1].id), appTreeEnvs, superPerson))
    when: "i delete environment 2"
      envApi.delete(envs[1].id)
    then:
      envApi.get(envs[2].id, Opts.empty(), superPerson).priorEnvironmentId == envs[0].id
  }


  def "if have one environments and one deleted environment, i cannot reorder a deleted environment"() {
    given: "i create five new environments in a tree"
      List<Environment> envs = []
      envs.add(envApi.create(new Environment().name("env-1-prior-7").description("1"), appTreeEnvs, superPerson))
      envs.add(envApi.create(new Environment().name("env-2-prior-7").description("2").priorEnvironmentId(envs[0].id), appTreeEnvs, superPerson))
      envApi.delete(envs[1].id)
      envs[1] = envApi.get(envs[1].id, Opts.empty(), superPerson)  // update the version no.
    when: "i set the environments to point correctly"
      envs[0].priorEnvironmentId = null
      envs[1].priorEnvironmentId = envs[0].id
      def result = envApi.setOrdering(appTreeEnvs, envs);
    then:
      result == null
  }

  def "i have one deleted environment, and two envs which i try and reorder, the result should be 2 environments"() {
    given: "i create five new environments in a tree"
      List<Environment> envs = []
      envs.add(envApi.create(new Environment().name("env-1-prior-7").description("1"), appTreeEnvs, superPerson))
      envs.add(envApi.create(new Environment().name("env-2-prior-7").description("2").priorEnvironmentId(envs[0].id), appTreeEnvs, superPerson))
      def delEnv = envApi.create(new Environment().name("env-3-prior-7").description("2").priorEnvironmentId(envs[1].id), appTreeEnvs, superPerson)
      envApi.delete(delEnv.id)
    when: "i set the environments to point correctly"
      envs[0].priorEnvironmentId = null
      envs[1].priorEnvironmentId = envs[0].id
      def result = envApi.setOrdering(appTreeEnvs, envs);
    then:
      result.size() == 2
  }

  def "i can create three environments and use set order and then reset them to empty"() {
    given: "i create five new environments in a tree"
      List<Environment> envs = []
      envs.add(envApi.create(new Environment().name("env-1-prior-8").description("1"), appTreeEnvs, superPerson))
      envs.add(envApi.create(new Environment().name("env-2-prior-8").description("2"), appTreeEnvs, superPerson))
      envs.add(envApi.create(new Environment().name("env-3-prior-8").description("3"), appTreeEnvs, superPerson))
    when: "i set the environments to point to each other"
      envs[0].priorEnvironmentId = null
      envs[1].priorEnvironmentId = envs[0].id
      envs[2].priorEnvironmentId = envs[1].id
      def result = envApi.setOrdering(appTreeEnvs, envs);
    and: "then set them to null and save them again"
      result.each { e -> e.priorEnvironmentId = null }
      def result2 = envApi.setOrdering(appTreeEnvs, result)
    then:
      result.size() == 3
      result2.find({e -> e.priorEnvironmentId != null}) == null
  }

  def "a new person in a new group that is not attached to environments has no roles, changing to reflects the new roles, admin always has all roles"() {
    given: "i have an average joe"
      def averageJoe = new DbPerson.Builder().email("averagejoe-env-1@featurehub.io").name("Average Joe").build()
      db.save(averageJoe)
      def averageJoeMemberOfPortfolio1 = convertUtils.toPerson(averageJoe)
    and: "i create a general portfolio group"
      groupInPortfolio1 = groupSqlApi.createGroup(portfolio1.id, new Group().name("envspec-p1-plain-portfolio-group"), superPerson)
      groupSqlApi.addPersonToGroup(groupInPortfolio1.id, averageJoeMemberOfPortfolio1.id.id, Opts.empty())
    and: "i have an environment"
      def env = envApi.create(new Environment().name("env-1-perm-1").description("1"), app1, superPerson)
    when: "i ask for the roles"
      def perms = envApi.personRoles(averageJoeMemberOfPortfolio1, env.id)
      def permsWhenNonAdmin = envApi.personRoles(superPerson, env.id)
    and: "I change the perms for the environment"
      def g = groupSqlApi.getGroup(groupInPortfolio1.id, Opts.opts(FillOpts.Members), superPerson)
//      g.members.add(averageJoeMemberOfPortfolio1)
      g.environmentRoles.add(new EnvironmentGroupRole().environmentId(env.id).roles([RoleType.CHANGE_VALUE]))
      groupSqlApi.updateGroup(g.id, g, null, false, false, true, Opts.empty())
      def perms2 = envApi.personRoles(averageJoeMemberOfPortfolio1, env.id)
      def permsAdmin = envApi.personRoles(superPerson, env.id)
    then: "the permissions to the portfolio are empty"
      perms.environmentRoles.isEmpty()
      perms.applicationRoles.isEmpty()
      perms2.applicationRoles.isEmpty()
      perms2.environmentRoles.containsAll([RoleType.CHANGE_VALUE, RoleType.READ])
      permsAdmin.applicationRoles.containsAll(ApplicationRoleType.values() as List)
      permsAdmin.environmentRoles.containsAll(RoleType.values() as List)
      permsWhenNonAdmin.applicationRoles.containsAll(ApplicationRoleType.values() as List)
      permsWhenNonAdmin.environmentRoles.containsAll(RoleType.values() as List)
  }

  def "i create an environment and update it using the update2"() {
    given:
      def envInfo = ['cacheControl': 'private, none', 'webhookUrl': 'https://blah']
      def envInfoExtra = ['mgmt.my.server': '2']
      envInfoExtra.putAll(envInfo)
      def env = envApi.create(new Environment().name("env-update2").description("1")
        .environmentInfo(envInfoExtra), appTreeEnvs, superPerson)
    when: "i update it"
      def upd1 = envApi.updateEnvironment(env.id, new UpdateEnvironment().version(env.version).description("2"), Opts.opts(FillOpts.Details))
    and: "i set the env vars"
      def upd2EnvInfo = ['info': 'i-want']
      def upd2 = envApi.updateEnvironment(env.id, new UpdateEnvironment().version(upd1.version).environmentInfo(upd2EnvInfo), Opts.opts(FillOpts.Details))
    and: "i include management env info"
      def upd3EnvInfo = ['mgmt.server.derver': '1', 'cacheControl': 'nostore']
      def upd3 = envApi.updateEnvironment(env.id, new UpdateEnvironment().version(upd2.version).environmentInfo(upd3EnvInfo), Opts.opts(FillOpts.Details))
    then:
      env.name == 'env-update2'
      upd1.description == '2'
      upd1.name == 'env-update2'
      upd1.environmentInfo == envInfo
      upd2.environmentInfo == upd2EnvInfo
      upd2.description == '2'
      upd3.environmentInfo != upd3EnvInfo
      upd3.environmentInfo['cacheControl'] == 'nostore'
  }
}
