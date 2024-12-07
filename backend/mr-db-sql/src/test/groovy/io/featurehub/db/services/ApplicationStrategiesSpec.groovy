package io.featurehub.db.services

import io.featurehub.dacha.model.PublishAction
import io.featurehub.db.api.Opts
import io.featurehub.db.api.PersonFeaturePermission
import io.featurehub.db.model.DbApplicationRolloutStrategy
import io.featurehub.db.model.DbFeatureValue
import io.featurehub.db.model.DbPerson
import io.featurehub.db.model.DbStrategyForFeatureValue
import io.featurehub.db.model.RoleType
import io.featurehub.db.publish.CacheSourceFeatureGroupApi
import io.featurehub.mr.events.common.CacheSource
import io.featurehub.mr.model.ApplicationRoleType
import io.featurehub.mr.model.ApplicationRolloutStrategy
import io.featurehub.mr.model.CreateApplicationRolloutStrategy
import io.featurehub.mr.model.CreateFeature
import io.featurehub.mr.model.FeatureValue
import io.featurehub.mr.model.FeatureValueType
import io.featurehub.mr.model.RolloutStrategyInstance
import io.featurehub.mr.model.UpdateApplicationRolloutStrategy

class ApplicationStrategiesSpec extends Base3Spec {
  ApplicationRolloutStrategySqlApi applicationRolloutStrategySqlApi

  def setup() {
    cacheSource = Mock(CacheSource)
    archiveStrategy = Mock(ArchiveStrategy)
    internalFeatureApi = Mock(InternalFeatureApi)
    applicationRolloutStrategySqlApi = new ApplicationRolloutStrategySqlApi(convertUtils, archiveStrategy, internalFeatureApi)
    featureSqlApi = new FeatureSqlApi(convertUtils, cacheSource, rsValidator, featureMessagingCloudEventPublisher, Mock(CacheSourceFeatureGroupApi))
  }

  String createFeature() {
    def cFeature = new CreateFeature().name(ranName()).key(ranName()).valueType(FeatureValueType.BOOLEAN).description(ranName())

    applicationSqlApi.createApplicationFeature(app1.id, cFeature, superPerson, Opts.empty())

    return cFeature.key
  }

  PersonFeaturePermission allPermissions() {
    return  new PersonFeaturePermission.Builder().person(superPerson)
      .roles(io.featurehub.mr.model.RoleType.values().toList().toSet())
      .appRoles(ApplicationRoleType.values().toList().toSet())
      .build()
  }

  ApplicationRolloutStrategy createStrategy() {
    return applicationRolloutStrategySqlApi.createStrategy(app1.id,
      new CreateApplicationRolloutStrategy().name(ranName()), superPerson.id.id, Opts.empty()
    )
  }

  FeatureValue associateStrategyWithFeatureValue(String key, ApplicationRolloutStrategy strategy) {
    def fv = featureSqlApi.getFeatureValueForEnvironment(env1.id, key)

    fv.locked(false)
    fv.rolloutStrategyInstances.add(new RolloutStrategyInstance().value(true).strategyId(strategy.id))

    def updatedValue = featureSqlApi.updateFeatureValueForEnvironment(env1.id, key, fv, allPermissions())

    return updatedValue
  }

  def "full lifecycle of application strategies"() {
    given: "i have a feature for the application"
      String key = createFeature()
    and: "full permissions"
      def perms = allPermissions()
    and: "i have an application strategy"
      def strategy = createStrategy()
      def updateName = ranName()
    when: "i associate the strategy with the feature value in the default environment"
      def fv = associateStrategyWithFeatureValue(key, strategy)
    then:
      with(cacheSource) {
        1 * publishFeatureChange( { DbFeatureValue v ->
          v.sharedRolloutStrategies.size() == 1
        })
      }
      0 * _
    when: "i update the application strategy"
      applicationRolloutStrategySqlApi.updateStrategy(app1.id, strategy.id,
        new UpdateApplicationRolloutStrategy().name(updateName), superPerson.id.id, Opts.empty())
    then:
      1 * internalFeatureApi.updatedApplicationStrategy({ DbStrategyForFeatureValue fvStrategy ->
        fvStrategy.rolloutStrategy.strategy.name == updateName
      }, _)
  }

  def "when we update or delete the feature strategy it will update the feature values"() {
    given: "we create two features"
      def keys = [createFeature(), createFeature()]
    and: "we create one strategy"
      def strategy = createStrategy()
    and: "we associate the strategy with both features"
      keys.each { associateStrategyWithFeatureValue(it, strategy) }
    and: "we reset the mock"
      internalFeatureApi = Mock(InternalFeatureApi)
      applicationRolloutStrategySqlApi = new ApplicationRolloutStrategySqlApi(convertUtils, archiveStrategy, internalFeatureApi)
    when: "we update the strategy name"
      applicationRolloutStrategySqlApi.updateStrategy(app1.id, strategy.id, new UpdateApplicationRolloutStrategy().name(ranName()), superuser, Opts.empty())
    then: "there is no interaction with the features as nothing changed that was important"
      0 * internalFeatureApi.updatedApplicationStrategy(_, _)
    when: "we update the strategy percentage"
      applicationRolloutStrategySqlApi.updateStrategy(app1.id, strategy.id,
        new UpdateApplicationRolloutStrategy().percentage(1000), superuser, Opts.empty())
    then:
      2 * internalFeatureApi.updatedApplicationStrategy({ DbStrategyForFeatureValue fvStrategy ->
        keys.contains(fvStrategy.featureValue.feature.key)
      }, _)
    when:
      applicationRolloutStrategySqlApi.archiveStrategy(app1.id, strategy.id, superuser)
    then:
      1 * internalFeatureApi.detachApplicationStrategy({ DbStrategyForFeatureValue fv ->
        fv.featureValue.feature.key == keys.first()
      }, { DbApplicationRolloutStrategy s ->
        s.name != strategy.name
      }, _)
    then:
      1 * internalFeatureApi.detachApplicationStrategy({ DbStrategyForFeatureValue fv ->
        fv.featureValue.feature.key == keys.last()
      }, _, _)
  }
}
