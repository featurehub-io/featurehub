package io.featurehub.db.services

import groovy.transform.CompileStatic
import io.featurehub.db.api.MultiFeatureValueUpdate
import io.featurehub.db.api.Opts
import io.featurehub.db.api.PersonFeaturePermission
import io.featurehub.db.api.RolloutStrategyUpdate
import io.featurehub.db.model.DbApplication
import io.featurehub.db.model.DbApplicationFeature
import io.featurehub.db.model.DbApplicationRolloutStrategy
import io.featurehub.db.model.DbEnvironment
import io.featurehub.db.model.DbFeatureValue
import io.featurehub.db.model.DbFeatureValueVersion
import io.featurehub.db.model.DbFeatureValueVersionKey
import io.featurehub.db.model.DbStrategyForFeatureValue
import io.featurehub.db.model.SharedRolloutStrategyVersion
import io.featurehub.db.model.query.QDbApplication
import io.featurehub.db.model.query.QDbApplicationRolloutStrategy
import io.featurehub.db.model.query.QDbEnvironment
import io.featurehub.mr.model.ApplicationRolloutStrategy
import io.featurehub.mr.model.CreateApplicationRolloutStrategy
import io.featurehub.mr.model.FeatureValue
import io.featurehub.mr.model.FeatureValueType
import io.featurehub.mr.model.RoleType
import io.featurehub.mr.model.RolloutStrategy
import io.featurehub.mr.model.RolloutStrategyInstance
import org.apache.commons.lang3.RandomStringUtils

import java.time.LocalDateTime

class FeatureAuditingApplicationStrategiesSpec extends Base3Spec {
  DbFeatureValue currentFeature
  DbApplicationFeature feature
  DbEnvironment dbEnvironment
  DbApplication dbApplication
  boolean lockChanged
  Set<RoleType> defaultRoles
  boolean currentLock
  ApplicationRolloutStrategySqlApi applicationRolloutStrategySqlApi
  DbFeatureValueVersionKey histId

  def setup() {
    applicationRolloutStrategySqlApi = new ApplicationRolloutStrategySqlApi(convertUtils, cacheSource)

    dbEnvironment = findEnvironment(env1.id)
    dbApplication = findApplication(app1.id)

    lockChanged = false
    currentLock = false
    defaultRoles = FeatureAuditingBaseUnitSpec.rolesChangeValue
    feature = af()

    histId = new DbFeatureValueVersionKey(UUID.randomUUID(), 1)
  }

  @CompileStatic
  DbEnvironment findEnvironment(UUID id) {
    return new QDbEnvironment().id.eq(id).findOne()
  }

  @CompileStatic
  DbApplication findApplication(UUID id) {
    return new QDbApplication().id.eq(id).findOne()
  }

  String ranName() {
    return RandomStringUtils.randomAlphabetic(10)
  }

  DbApplicationFeature af(FeatureValueType type = FeatureValueType.BOOLEAN) {
    return new DbApplicationFeature.Builder().parentApplication(dbApplication).key(ranName()).name('choochoo').valueType(type).build()
  }

  DbFeatureValue featureValue(String val, DbApplicationFeature feat) {
    return new DbFeatureValue(dbSuperPerson, false, feat, dbEnvironment, val)
  }

  MultiFeatureValueUpdate<RolloutStrategyUpdate, RolloutStrategy> updateStrategies(List<DbStrategyForFeatureValue> current,
                                                                                   List<SharedRolloutStrategyVersion> historical,
                                                                                   List<RolloutStrategyInstance> updated, PersonFeaturePermission person) {
    currentFeature = featureValue("y", feature).with { it.locked = currentLock; it.sharedRolloutStrategies = current; it }

    return featureSqlApi.updateSelectivelyApplicationRolloutStrategies(
      person,
      new FeatureValue().rolloutStrategyInstances(updated),
      new DbFeatureValueVersion(histId, LocalDateTime.now(), dbSuperPerson, "y", false, false, [], historical, feature, 0),
      currentFeature, lockChanged, app1.id
    )
  }

  @CompileStatic
  RolloutStrategy toRolloutStrategy(ApplicationRolloutStrategy ars) {
    return new RolloutStrategy().name(ars.name).value(true).id(new QDbApplicationRolloutStrategy().id.eq(ars.id).findOne().shortUniqueCode)
  }

  def "when i add strategies i can"() {
    given:
      def sharedStrategy = applicationRolloutStrategySqlApi.createStrategy(app1.id,
        new CreateApplicationRolloutStrategy().name(ranName()), superuser, Opts.empty())
    and: "i have an incoming feature change"
      def incoming = [
        new RolloutStrategyInstance().name(sharedStrategy.name)
          .disabled(false).value(true).strategyId(sharedStrategy.id)]
    when:
      def result = updateStrategies([], [], incoming,
        new PersonFeaturePermission(superPerson, defaultRoles))
    then:
      result.hasChanged
      result.updated == [new RolloutStrategyUpdate("added", null, toRolloutStrategy(sharedStrategy))]
      result.previous == []
      currentFeature.sharedRolloutStrategies.find { it.rolloutStrategy.id == sharedStrategy.id }
  }
}
