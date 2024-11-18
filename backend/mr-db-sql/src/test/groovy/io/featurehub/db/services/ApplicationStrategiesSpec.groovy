package io.featurehub.db.services

import io.featurehub.dacha.model.PublishAction
import io.featurehub.db.api.Opts
import io.featurehub.db.api.PersonFeaturePermission
import io.featurehub.db.model.DbApplicationRolloutStrategy
import io.featurehub.db.model.DbFeatureValue
import io.featurehub.db.model.RoleType
import io.featurehub.db.publish.CacheSourceFeatureGroupApi
import io.featurehub.mr.events.common.CacheSource
import io.featurehub.mr.model.ApplicationRoleType
import io.featurehub.mr.model.CreateApplicationRolloutStrategy
import io.featurehub.mr.model.CreateFeature
import io.featurehub.mr.model.FeatureValue
import io.featurehub.mr.model.FeatureValueType
import io.featurehub.mr.model.RolloutStrategyInstance
import io.featurehub.mr.model.UpdateApplicationRolloutStrategy

class ApplicationStrategiesSpec extends Base3Spec {
  ApplicationRolloutStrategySqlApi applicationRolloutStrategySqlApi

  def setup() {
    applicationRolloutStrategySqlApi = new ApplicationRolloutStrategySqlApi(convertUtils, cacheSource)
  }

  def "full lifecycle of application strategies"() {
    given: "i update the cache source"
      cacheSource = Mock(CacheSource)
      applicationRolloutStrategySqlApi = new ApplicationRolloutStrategySqlApi(convertUtils, cacheSource)
      featureSqlApi = new FeatureSqlApi(convertUtils, cacheSource, rsValidator, featureMessagingCloudEventPublisher, Mock(CacheSourceFeatureGroupApi))
    and: "i have a feature for the application"
      def cFeature = new CreateFeature().name(ranName()).key(ranName()).valueType(FeatureValueType.BOOLEAN).description(ranName())
      def feature = applicationSqlApi.createApplicationFeature(app1.id,
        cFeature, superPerson, Opts.empty())
        .find({ cFeature.key == it.key} )
    and: "full permissions"
      def perms = new PersonFeaturePermission.Builder().person(superPerson)
        .roles(io.featurehub.mr.model.RoleType.values().toList().toSet())
        .appRoles(ApplicationRoleType.values().toList().toSet())
        .build()
    and: "i have an application strategy"
      def strategy = applicationRolloutStrategySqlApi.createStrategy(app1.id,
        new CreateApplicationRolloutStrategy().name(ranName()), superPerson.id.id, Opts.empty()
      )
//    then:
//      1 * cacheSource.publishApplicationRolloutStrategyChange( { DbApplicationRolloutStrategy rs -> rs.strategy.id == strategy.id })
    when: "i associate the strategy with the feature value in the default environment"
      def fv = featureSqlApi.getFeatureValueForEnvironment(env1.id, cFeature.key)

      fv.locked(false)
      fv.rolloutStrategyInstances.add(new RolloutStrategyInstance().value(true).strategyId(strategy.id))

      def updatedValue = featureSqlApi.updateFeatureValueForEnvironment(env1.id, cFeature.key, fv, perms)
    then:
      with(cacheSource) {
        1 * publishFeatureChange( { DbFeatureValue v ->
          v.sharedRolloutStrategies.size() == 1
        })
      }
      0 * _
    when: "i update the application strategy"
      strategy.name(ranName())
      applicationRolloutStrategySqlApi.updateStrategy(app1.id, strategy.id,
        new UpdateApplicationRolloutStrategy().name(ranName()), superPerson.id.id, Opts.empty())
    then:
      1 * cacheSource.publishApplicationRolloutStrategyChange(PublishAction.UPDATE,
        { DbApplicationRolloutStrategy rs -> rs.strategy.id == strategy.id })
  }
}
