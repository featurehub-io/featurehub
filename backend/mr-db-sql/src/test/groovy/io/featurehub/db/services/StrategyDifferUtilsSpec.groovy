package io.featurehub.db.services

import io.featurehub.db.api.Opts
import io.featurehub.db.api.PersonFeaturePermission
import io.featurehub.db.api.RolloutStrategyApi
import io.featurehub.db.api.RolloutStrategyValidator
import io.featurehub.db.model.DbPortfolio
import io.featurehub.db.model.query.QDbOrganization
import io.featurehub.db.publish.CacheSource
import io.featurehub.db.services.strategies.StrategyDiffer
import io.featurehub.db.services.strategies.StrategyDifferUtils
import io.featurehub.mr.model.Application
import io.featurehub.mr.model.ApplicationRoleType
import io.featurehub.mr.model.Environment
import io.featurehub.mr.model.Feature
import io.featurehub.mr.model.FeatureValue
import io.featurehub.mr.model.FeatureValueType
import io.featurehub.mr.model.RoleType
import io.featurehub.mr.model.RolloutStrategy
import io.featurehub.mr.model.RolloutStrategyInstance
import spock.lang.Shared

/**
 * All of the testing in here is around detecting changes required for feature value linking to shared
 * rollout strategies at the application level. So we need
 * to have an application we can do that with.
 *
 * We aren't testing feature creation or feature value creation, we are *just* testing diffing.
 */
class StrategyDifferUtilsSpec extends BaseSpec {
  @Shared DbPortfolio portfolio1
  @Shared Application app1
  @Shared String app1Id
  @Shared FeatureSqlApi featureSqlApi
  @Shared RolloutStrategyApi rolloutStrategyApi
  @Shared ApplicationSqlApi appApi
  @Shared String envId

  def setupSpec() {
    baseSetupSpec()

    appApi = new ApplicationSqlApi(database, convertUtils, Mock(CacheSource), archiveStrategy)

    // now set up the environments we need
    portfolio1 = new DbPortfolio.Builder().name("p1-app-feature").whoCreated(dbSuperPerson).organization(new QDbOrganization().findOne()).build()

    database.save(portfolio1)

    app1 = appApi.createApplication(portfolio1.id.toString(), new Application().name("strategy-diff").description('desc'), superPerson)
    app1Id = app1.id
    def environmentSqlApi = new EnvironmentSqlApi(database, convertUtils, Mock(CacheSource), archiveStrategy)
    envId = environmentSqlApi.create(new Environment().name("strategy-diff-env-1"), new Application().id(app1.id), superPerson).id

    featureSqlApi = new FeatureSqlApi(database, convertUtils, Mock(CacheSource), Mock(RolloutStrategyValidator), new StrategyDifferUtils())
    rolloutStrategyApi = new RolloutStrategySqlApi(database, convertUtils, Mock(CacheSource))
  }

  def "i can track a series of expected changed from the creation of a boolean feature value adding, updating and removing shared strategies and full removal"() {
    given: "i have a feature"
      def key = 'FV_SPRING_IS_HERE'
      def feature = appApi.createApplicationFeature(app1.id, new Feature().name(key).valueType(FeatureValueType.BOOLEAN).key(key), superPerson)
    and: "a feature value in an environment"
      def perms = new PersonFeaturePermission(superPerson, [RoleType.CHANGE_VALUE, RoleType.UNLOCK, RoleType.LOCK] as Set<RoleType>)
      def fvLocked = featureSqlApi.getFeatureValueForEnvironment(envId, key)
    and: "i unlock it so i can make changes"
      def fv = featureSqlApi.updateFeatureValueForEnvironment(envId, key, fvLocked.locked(false), perms)
    and: "i add two shared rollout strategies"
      def rs1 = rolloutStrategyApi.createStrategy(app1Id, new RolloutStrategy().name("will"), superPerson, Opts.empty())
      def rs2 = rolloutStrategyApi.createStrategy(app1Id, new RolloutStrategy().name("grace"), superPerson, Opts.empty())
    when: "i update the feature with one"
      fv.rolloutStrategyInstances([new RolloutStrategyInstance().value(Boolean.FALSE).strategyId(rs1.rolloutStrategy.id)])
      def withOne = featureSqlApi.updateFeatureValueForEnvironment(envId, key, fv, perms)
      def fv1 = featureSqlApi.getFeatureValueForEnvironment(envId, key) // validate
    and: "then I update and add the second one"
      def withTwo = featureSqlApi.updateFeatureValueForEnvironment(envId, key, withOne.addRolloutStrategyInstancesItem(new RolloutStrategyInstance().strategyId(rs2.rolloutStrategy.id).value(Boolean.TRUE)), perms)
      def fv2 = featureSqlApi.getFeatureValueForEnvironment(envId, key) // validate
    and: "then remove the first one and change the second"
      def withThree = featureSqlApi.updateFeatureValueForEnvironment(envId, key, withTwo.rolloutStrategyInstances([new RolloutStrategyInstance().strategyId(rs2.rolloutStrategy.id).value(Boolean.FALSE).disabled(true)]), perms)
      def fv3 = featureSqlApi.getFeatureValueForEnvironment(envId, key) // validate
    and: "then i fully remove all attachments"
      featureSqlApi.updateFeatureValueForEnvironment(envId, key, withThree.rolloutStrategyInstances([]), perms)
      def fv4 = featureSqlApi.getFeatureValueForEnvironment(envId, key) // validate
    then:
      fv1.rolloutStrategyInstances.size() == 1
      fv1.rolloutStrategyInstances[0].value == Boolean.FALSE
      fv1.rolloutStrategyInstances[0].strategyId == rs1.rolloutStrategy.id
      fv2.rolloutStrategyInstances.size() == 2
      fv2.rolloutStrategyInstances.find({it.strategyId == rs1.rolloutStrategy.id}).value == Boolean.FALSE
      fv2.rolloutStrategyInstances.find({it.strategyId == rs1.rolloutStrategy.id}).disabled == null
      fv2.rolloutStrategyInstances.find({it.strategyId == rs2.rolloutStrategy.id}).value == Boolean.TRUE
      fv2.rolloutStrategyInstances.find({it.strategyId == rs2.rolloutStrategy.id}).disabled == null
      fv3.rolloutStrategyInstances.size() == 1
      fv3.rolloutStrategyInstances.find({it.strategyId == rs2.rolloutStrategy.id}).value == Boolean.FALSE
      fv3.rolloutStrategyInstances.find({it.strategyId == rs2.rolloutStrategy.id}).disabled == Boolean.TRUE
      fv4.rolloutStrategyInstances.isEmpty()
  }

  def "i can track a series of expected changed from the creation of a string feature value adding, updating and removing shared strategies and full removal"() {
    given: "i have a feature"
      def key = 'FV_SEPT_IS_HERE'
      def feature = appApi.createApplicationFeature(app1.id, new Feature().name(key).valueType(FeatureValueType.STRING).key(key), superPerson)
    and: "a feature value in an environment"
      def perms = new PersonFeaturePermission(superPerson, [RoleType.CHANGE_VALUE, RoleType.UNLOCK, RoleType.LOCK] as Set<RoleType>)
    and: "i add two shared rollout strategies"
      def rs1 = rolloutStrategyApi.createStrategy(app1Id, new RolloutStrategy().name("aunt"), superPerson, Opts.empty())
      def rs2 = rolloutStrategyApi.createStrategy(app1Id, new RolloutStrategy().name("mary"), superPerson, Opts.empty())
    when: "i create the feature with the first shared strategy"
      def fv = new FeatureValue()
        .rolloutStrategyInstances([new RolloutStrategyInstance().value("shimmy").strategyId(rs1.rolloutStrategy.id)])
        .locked(false)
        .valueString("georgie")
        .key(key)
      def withOne = featureSqlApi.createFeatureValueForEnvironment(envId, key,
        fv,
        perms
      )

      def fv1 = featureSqlApi.getFeatureValueForEnvironment(envId, key) // validate
    and: "then I update and add the second one"
      def withTwo = featureSqlApi.updateFeatureValueForEnvironment(envId, key, withOne.addRolloutStrategyInstancesItem(new RolloutStrategyInstance().strategyId(rs2.rolloutStrategy.id).value("ook")), perms)
      def fv2 = featureSqlApi.getFeatureValueForEnvironment(envId, key) // validate
    and: "then remove the first one and change the second"
      def withThree = featureSqlApi.updateFeatureValueForEnvironment(envId, key, withTwo.rolloutStrategyInstances([new RolloutStrategyInstance().strategyId(rs2.rolloutStrategy.id).value("library").disabled(true)]), perms)
      def fv3 = featureSqlApi.getFeatureValueForEnvironment(envId, key) // validate
    and: "then i fully remove all attachments"
      featureSqlApi.updateFeatureValueForEnvironment(envId, key, withThree.rolloutStrategyInstances([]), perms)
      def fv4 = featureSqlApi.getFeatureValueForEnvironment(envId, key) // validate
    then:
      fv1.rolloutStrategyInstances.size() == 1
      fv1.rolloutStrategyInstances[0].value == "shimmy"
      fv1.rolloutStrategyInstances[0].strategyId == rs1.rolloutStrategy.id
      fv2.rolloutStrategyInstances.size() == 2
      fv2.rolloutStrategyInstances.find({it.strategyId == rs1.rolloutStrategy.id}).value == "shimmy"
      fv2.rolloutStrategyInstances.find({it.strategyId == rs1.rolloutStrategy.id}).name == "aunt"
      fv2.rolloutStrategyInstances.find({it.strategyId == rs1.rolloutStrategy.id}).disabled == null
      fv2.rolloutStrategyInstances.find({it.strategyId == rs2.rolloutStrategy.id}).value == "ook"
      fv2.rolloutStrategyInstances.find({it.strategyId == rs2.rolloutStrategy.id}).name == "mary"
      fv2.rolloutStrategyInstances.find({it.strategyId == rs2.rolloutStrategy.id}).disabled == null
      fv3.rolloutStrategyInstances.size() == 1
      fv3.rolloutStrategyInstances.find({it.strategyId == rs2.rolloutStrategy.id}).value == "library"
      fv3.rolloutStrategyInstances.find({it.strategyId == rs2.rolloutStrategy.id}).disabled == Boolean.TRUE
      fv4.rolloutStrategyInstances.isEmpty()
  }
}
