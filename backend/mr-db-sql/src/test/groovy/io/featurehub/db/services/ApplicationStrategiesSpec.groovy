package io.featurehub.db.services


import io.featurehub.db.api.Opts
import io.featurehub.db.api.PersonFeaturePermission
import io.featurehub.db.messaging.FeatureMessagingParameter
import io.featurehub.db.messaging.FeatureMessagingPublisher
import io.featurehub.db.model.DbApplicationRolloutStrategy
import io.featurehub.db.model.DbFeatureValue
import io.featurehub.db.model.DbStrategyForFeatureValue
import io.featurehub.db.publish.CacheSourceFeatureGroupApi
import io.featurehub.mr.events.common.CacheSource
import io.featurehub.mr.model.ApplicationRoleType
import io.featurehub.mr.model.ApplicationRolloutStrategy
import io.featurehub.mr.model.CreateApplicationRolloutStrategy
import io.featurehub.mr.model.CreateFeature
import io.featurehub.mr.model.FeatureValue
import io.featurehub.mr.model.FeatureValueType
import io.featurehub.mr.model.RolloutStrategy
import io.featurehub.mr.model.RolloutStrategyInstance
import io.featurehub.mr.model.UpdateApplicationRolloutStrategy
import spock.lang.Rollup
import spock.lang.Unroll

class ApplicationStrategiesSpec extends Base3Spec {
  ApplicationRolloutStrategySqlApi applicationRolloutStrategySqlApi

  def setup() {
    cacheSource = Mock(CacheSource)
    archiveStrategy = Mock(ArchiveStrategy)
    internalFeatureApi = Mock(InternalFeatureApi)
    applicationRolloutStrategySqlApi = new ApplicationRolloutStrategySqlApi(convertUtils, internalFeatureApi)
    UpdateFeatureApi updateFeatureApi = new UpdateFeatureApiImpl(convertUtils, cacheSource, featureMessagingCloudEventPublisher)
    featureSqlApi = new FeatureSqlApi(convertUtils, rsValidator, Mock(CacheSourceFeatureGroupApi), updateFeatureApi)
  }

  String createFeature(FeatureValueType valueType = FeatureValueType.BOOLEAN) {
    def cFeature = new CreateFeature().name(ranName()).key(ranName()).valueType(valueType).description(ranName())

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

  FeatureValue associateStrategyWithFeatureValue(String key, ApplicationRolloutStrategy strategy, Object val) {
    def fv = featureSqlApi.getFeatureValueForEnvironment(env1.id, key)

    if (fv == null) {
      fv = new FeatureValue().key(key).rolloutStrategyInstances([])
    }

    fv.value(val)
    fv.locked(false)
    fv.rolloutStrategyInstances.add(new RolloutStrategyInstance().value(val).strategyId(strategy.id))

    def updatedValue = featureSqlApi.updateFeatureValueForEnvironment(env1.id, key, fv, allPermissions())

    return updatedValue
  }

  @Unroll
  def "full lifecycle of application strategies"() {
    given: "i have a feature for the application"
      String key = createFeature(featureType)
    and: "i have an application strategy"
      def strategy = createStrategy()

      def updateName = ranName()
    when: "i associate the strategy with the feature value in the default environment"
      associateStrategyWithFeatureValue(key, strategy, val)
    then:
      with(cacheSource) {
        1 * publishFeatureChange( { DbFeatureValue v ->
          v.sharedRolloutStrategies.size() == 1
        })
      }
      0 * _
    when: "i update the application strategy"
      applicationRolloutStrategySqlApi.updateStrategy(app1.id, strategy.id,
        new UpdateApplicationRolloutStrategy().percentage(20).name(updateName), superPerson.id.id, Opts.empty())
    then:
      1 *
      1 * internalFeatureApi.updatedApplicationStrategy({ DbStrategyForFeatureValue fvStrategy ->
        fvStrategy.rolloutStrategy.strategy.name == updateName
        fvStrategy.rolloutStrategy.strategy.percentage == 20
      }, { RolloutStrategy original ->
        original != null
        original.percentage == null
        original.name == strategy.name
      }, _)
    where:
      featureType | val
      FeatureValueType.BOOLEAN | false
      FeatureValueType.BOOLEAN | true
      FeatureValueType.NUMBER | null
      FeatureValueType.NUMBER | 5
      FeatureValueType.STRING | null
      FeatureValueType.STRING | "x"
      FeatureValueType.JSON | null
      FeatureValueType.JSON | "x"
  }

  def "when we update or delete the feature strategy it will update the feature values"() {
    given: "we create two features"
      def keys = [createFeature(), createFeature()]
    and: "we create one strategy"
      def strategy = createStrategy()
    and: "we associate the strategy with both features"
      keys.each { associateStrategyWithFeatureValue(it, strategy, false) }
    and: "we reset the mock"
      internalFeatureApi = Mock(InternalFeatureApi)
      applicationRolloutStrategySqlApi = new ApplicationRolloutStrategySqlApi(convertUtils, internalFeatureApi)
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
      }, { RolloutStrategy rs -> rs.percentage == null }, _)
    when:
      applicationRolloutStrategySqlApi.archiveStrategy(app1.id, strategy.id, superuser)
    then: // if we do this in separate then:'s then we can recognize the different calls the same method
      1 * internalFeatureApi.detachApplicationStrategy({ DbStrategyForFeatureValue fv ->
        fv.featureValue.feature.key == keys.first()
      }, { RolloutStrategy rs -> rs.percentage == 1000 },  _)
    then:
      1 * internalFeatureApi.detachApplicationStrategy({ DbStrategyForFeatureValue fv ->
        fv.featureValue.feature.key == keys.last()
      }, _, _)
  }

  def "when we use the real internal feature service and update and delete strategies"() {
    given: "we create two features"
      def keys = [createFeature()]
    and: "we create one strategy"
      def strategy = createStrategy()
    and: "we associate the strategy with both features"
      keys.each { associateStrategyWithFeatureValue(it, strategy, false) }
    and: "we ensure we are not using a mock internal feature service"
      FeatureMessagingPublisher fmp = Mock()
      internalFeatureApi = new InternalFeatureSqlApi(convertUtils, cacheSource, fmp)
      applicationRolloutStrategySqlApi = new ApplicationRolloutStrategySqlApi(convertUtils, internalFeatureApi)
    when: "we update the strategy percentage"
      applicationRolloutStrategySqlApi.updateStrategy(app1.id, strategy.id,
        new UpdateApplicationRolloutStrategy().percentage(1000), superuser, Opts.empty())
    then:
      1 * cacheSource.publishFeatureChange({ DbFeatureValue fv ->
        fv.feature.key == keys.first()
        fv.version == 3
      })
      1 * fmp.publish({ FeatureMessagingParameter p ->
        p.applicationStrategyUpdates.hasChanged
        p.applicationStrategyUpdates.updated[0].new.percentage == 1000
        p.applicationStrategyUpdates.updated[0].old.percentage == null
      }, _)
    when: "we delete the application strategy"
      applicationRolloutStrategySqlApi.archiveStrategy(app1.id, strategy.id, superuser)
    then:
      1 * cacheSource.publishFeatureChange({ DbFeatureValue fv ->
        fv.feature.key == keys.first()
        fv.version == 4
      })
      1 * fmp.publish({ FeatureMessagingParameter p ->
        p.applicationStrategyUpdates.hasChanged
        p.applicationStrategyUpdates.updated[0].new == null
        p.applicationStrategyUpdates.updated[0].old.percentage == 1000
      }, _)
  }
}
