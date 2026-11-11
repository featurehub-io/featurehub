package io.featurehub.db.publish

import io.ebean.DB
import io.ebean.datasource.DataSourceConfig
import io.featurehub.dacha.model.PublishEnvironment
import io.featurehub.dacha.model.PublishServiceAccount
import io.featurehub.db.model.DbApplication
import io.featurehub.db.model.DbEnvironment
import io.featurehub.db.model.DbOrganization
import io.featurehub.db.model.DbPerson
import io.featurehub.db.model.DbPortfolio
import io.featurehub.db.model.DbServiceAccount
import io.featurehub.db.model.DbServiceAccountEnvironment
import io.featurehub.db.services.Conversions
import io.featurehub.mr.events.common.CacheBroadcast
import io.featurehub.mr.model.RoleType
import io.featurehub.utils.ExecutorSupplier
import org.glassfish.hk2.api.IterableProvider
import spock.lang.Shared
import spock.lang.Specification

import java.time.Instant
import java.time.LocalDateTime
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.Executors

/**
 * Everything that decides what reaches a cache (the full publish to dacha2, the Redis walk/sweep, the dacha2 on-demand
 * lookups and the publishable api key list) must agree: only environments and service accounts that are not archived,
 * not unpublished and in an organisation that is not archived.
 */
class DbCacheSourcePublishableSpec extends Specification {
  static class Org {
    DbOrganization org
    DbEnvironment env
    DbServiceAccount sa
  }

  @Shared Org live
  @Shared Org archivedOrg
  @Shared DbEnvironment unpublishedEnv
  @Shared DbEnvironment archivedEnv
  @Shared DbServiceAccount unpublishedSa
  @Shared DbServiceAccount archivedSa
  @Shared DbServiceAccount noPermissionsSa

  ConcurrentLinkedQueue<PublishEnvironment> envsPublished
  ConcurrentLinkedQueue<PublishServiceAccount> sasPublished
  DbCacheSource cacheSource

  def setupSpec() {
    System.setProperty("ebean.ddl.generate", "true")
    System.setProperty("ebean.ddl.run", "true")
    DB.getDefault()

    def person = new DbPerson.Builder().name("publisher").email("publisher@publishable.test").build()
    person.save()

    live = createOrg("live", person)
    archivedOrg = createOrg("archived", person)
    archivedOrg.org.whenArchived = LocalDateTime.now()
    archivedOrg.org.save()

    def portfolio = live.sa.portfolio
    def app = live.env.parentApplication

    unpublishedEnv = env("unpublished-env", app, person)
    unpublishedEnv.whenUnpublished = Instant.now()
    unpublishedEnv.save()
    archivedEnv = env("archived-env", app, person)
    archivedEnv.whenArchived = LocalDateTime.now()
    archivedEnv.save()

    unpublishedSa = sa("unpublished-sa", portfolio, person, live.env, "READ")
    unpublishedSa.whenUnpublished = Instant.now()
    unpublishedSa.save()
    archivedSa = sa("archived-sa", portfolio, person, live.env, "READ")
    archivedSa.whenArchived = LocalDateTime.now()
    archivedSa.save()

    noPermissionsSa = sa("no-permissions-sa", portfolio, person, live.env, "")

    // the live service account can also see the environments that are not publishable
    [unpublishedEnv, archivedEnv].each { e ->
      new DbServiceAccountEnvironment.Builder().serviceAccount(live.sa).environment(e).permissions("READ").build().save()
    }
  }

  static Org createOrg(String name, DbPerson person) {
    def org = new DbOrganization.Builder().name("org-${name}").build()
    org.save()
    def portfolio = new DbPortfolio.Builder().name("portfolio-${name}").description("p").organization(org).whoCreated(person).build()
    portfolio.save()
    def app = new DbApplication.Builder().name("app-${name}").description("a").portfolio(portfolio).whoCreated(person).build()
    app.save()
    def e = env("env-${name}", app, person)
    def s = sa("sa-${name}", portfolio, person, e, "READ")
    return new Org(org: org, env: e, sa: s)
  }

  static DbEnvironment env(String name, DbApplication app, DbPerson person) {
    def e = new DbEnvironment.Builder().name(name).description("e").parentApplication(app).whoCreated(person).build()
    e.save()
    return e
  }

  static DbServiceAccount sa(String name, DbPortfolio portfolio, DbPerson person, DbEnvironment e, String permissions) {
    def s = new DbServiceAccount(person, person, name, null, "server-${name}", "client*${name}", portfolio)
    s.save()
    new DbServiceAccountEnvironment.Builder().serviceAccount(s).environment(e).permissions(permissions).build().save()
    return s
  }

  def setup() {
    envsPublished = new ConcurrentLinkedQueue<>()
    sasPublished = new ConcurrentLinkedQueue<>()

    def broadcaster = Stub(CacheBroadcast) {
      publishEnvironment(_) >> { args -> envsPublished.add(args[0] as PublishEnvironment) }
      publishServiceAccount(_) >> { args -> sasPublished.add(args[0] as PublishServiceAccount) }
    }
    def broadcasters = Stub(IterableProvider) {
      iterator() >> { [broadcaster].iterator() }
    }
    def conversions = Stub(Conversions) {
      splitServiceAccountPermissions(_) >> { args -> args[0] ? [RoleType.READ] : [] }
    }
    def featureGroupApi = Stub(CacheSourceFeatureGroupApi) {
      collectStrategiesFromGroupsForEnvironment(_) >> [:]
      collectStrategiesFromGroupsForEnvironmentFeature(_, _) >> []
    }
    def executorSupplier = Stub(ExecutorSupplier) {
      executorService(_) >> Executors.newFixedThreadPool(2)
    }
    def dsConfig = new DataSourceConfig()
    dsConfig.maxConnections = 4

    cacheSource = new DbCacheSource(conversions, dsConfig, broadcasters as IterableProvider<CacheBroadcast>,
      featureGroupApi, Stub(FeatureModelWalker), Stub(CommonCacheGenerator), executorSupplier)
    cacheSource.publishInit()
  }

  def "the walker only sees the publishable environments and service accounts"() {
    expect:
      cacheSource.publishableEnvironmentIds() == [live.env.id]
      cacheSource.publishableServiceAccountIds() as Set == [live.sa.id, noPermissionsSa.id] as Set
  }

  def "the full publish only sends the publishable environments and service accounts"() {
    when:
      cacheSource.publishToStreamingCaches()
    and: "environments are filled on the executor, wait for them"
      def deadline = System.currentTimeMillis() + 10000
      while (envsPublished.isEmpty() && System.currentTimeMillis() < deadline) {
        Thread.sleep(20)
      }
      Thread.sleep(200)
    then:
      envsPublished*.environment*.id == [live.env.id]
      sasPublished*.serviceAccount*.id as Set == [live.sa.id, noPermissionsSa.id] as Set
    and: "the live service account's permissions do not include the unpublished or archived environments"
      sasPublished.find { it.serviceAccount.id == live.sa.id }.serviceAccount.permissions*.environmentId == [live.env.id]
  }

  def "a single environment or service account is only found when it is publishable"() {
    expect:
      cacheSource.publishableEnvironment(live.env.id)?.environment?.id == live.env.id
      [unpublishedEnv, archivedEnv, archivedOrg.env].every { cacheSource.publishableEnvironment(it.id) == null }
      cacheSource.publishableServiceAccount(live.sa.id)?.id == live.sa.id
      [unpublishedSa, archivedSa, archivedOrg.sa].every { cacheSource.publishableServiceAccount(it.id) == null }
  }

  def "dacha2 on-demand lookups do not return anything that is not publishable"() {
    expect: "environments by id"
      cacheSource.getEnvironment(live.env.id)?.environment?.id == live.env.id
      [unpublishedEnv, archivedEnv, archivedOrg.env].every { cacheSource.getEnvironment(it.id) == null }
    and: "service accounts by either api key"
      cacheSource.getServiceAccount(live.sa.apiKeyServerEval)?.id == live.sa.id
      cacheSource.getServiceAccount(live.sa.apiKeyClientEval)?.id == live.sa.id
      [unpublishedSa, archivedSa, archivedOrg.sa].every { s ->
        cacheSource.getServiceAccount(s.apiKeyServerEval) == null && cacheSource.getServiceAccount(s.apiKeyClientEval) == null
      }
  }

  def "publishable api keys only pair publishable environments with publishable service accounts that have permissions"() {
    expect:
      cacheSource.publishableApiKeys() == ["${live.env.id}/${live.sa.apiKeyClientEval}".toString()]
  }
}
