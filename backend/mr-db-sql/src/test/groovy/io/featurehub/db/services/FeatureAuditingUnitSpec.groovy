package io.featurehub.db.services

import io.ebean.Database
import io.featurehub.db.api.FeatureApi
import io.featurehub.db.api.LockedException
import io.featurehub.db.api.OptimisticLockingException
import io.featurehub.db.api.PersonFeaturePermission
import io.featurehub.db.api.RolloutStrategyValidator
import io.featurehub.db.model.DbApplicationFeature
import io.featurehub.db.model.DbFeatureValueVersion
import io.featurehub.db.model.DbFeatureValue
import io.featurehub.db.model.DbFeatureValueVersionKey
import io.featurehub.db.model.DbPerson
import io.featurehub.db.model.FeatureState
import io.featurehub.db.services.strategies.StrategyDiffer
import io.featurehub.mr.events.common.CacheSource
import io.featurehub.mr.model.FeatureValue
import io.featurehub.mr.model.FeatureValueType
import io.featurehub.mr.model.Person
import io.featurehub.mr.model.RoleType
import spock.lang.Specification

import java.time.LocalDateTime

/**
 * we are testing the methods internally in isolation
 */
class FeatureAuditingUnitSpec extends Specification {
  Database database
  Conversions conversions
  CacheSource cacheSource
  RolloutStrategyValidator rolloutStrategyValidator
  StrategyDiffer strategyDiffer

  FeatureSqlApi fsApi
  Person person
  DbPerson dbPerson
  DbFeatureValueVersionKey histId

  def setup() {
    database = Mock()
    conversions = Mock()
    cacheSource = Mock()
    rolloutStrategyValidator = Mock()
    strategyDiffer = Mock()
    person = new Person()
    dbPerson = new DbPerson.Builder().build()

    histId = new DbFeatureValueVersionKey(UUID.randomUUID(), 1)

    fsApi = new FeatureSqlApi(database, conversions, cacheSource, rolloutStrategyValidator, strategyDiffer)
  }

  final rolesChangeValue = [RoleType.CHANGE_VALUE] as Set<RoleType>
  final rolesRead = [RoleType.READ] as Set<RoleType>

  def "bool - they pass an update the same as the existing one but different from the historical one"() {
    when:
      def result = fsApi.updateSelectivelyDefaultValue(
        new DbApplicationFeature.Builder().valueType(FeatureValueType.BOOLEAN).build(),
        new FeatureValue().valueBoolean(true),
        new DbFeatureValueVersion(histId, LocalDateTime.now(), dbPerson, FeatureState.READY, "false", false, false, [], []),
        new DbFeatureValue.Builder().defaultValue("true").build(),
        new PersonFeaturePermission(new Person(), rolesChangeValue), false
      )
    then:
      !result
  }

  def "bool - they pass an update the same as the historical one but different from the existing one"() {
    when:
      def result = fsApi.updateSelectivelyDefaultValue(
        new DbApplicationFeature.Builder().valueType(FeatureValueType.BOOLEAN).build(),
        new FeatureValue().valueBoolean(false),
        new DbFeatureValueVersion(histId, LocalDateTime.now(), dbPerson, FeatureState.READY, "false", false, false, [], []),
        new DbFeatureValue.Builder().defaultValue("true").build(),
        new PersonFeaturePermission(new Person(), rolesChangeValue), false
      )
    then:
      !result
  }

  def "string - they pass an update the same as the historical one but different from the existing one"() {
    when:
      def result = fsApi.updateSelectivelyDefaultValue(
        new DbApplicationFeature.Builder().valueType(FeatureValueType.STRING).build(),
        new FeatureValue().valueString("x"),
        new DbFeatureValueVersion(histId, LocalDateTime.now(), dbPerson, FeatureState.READY, "x", false, false, [], []),
        new DbFeatureValue.Builder().defaultValue("y").build(),
        new PersonFeaturePermission(new Person(), rolesChangeValue), false
      )
    then:
      !result
  }

  def "string - they pass an update the different to the historical one and historical is the same as existing one"() {
    given:
      def existing = new DbFeatureValue.Builder().defaultValue("y").build()
    when:
      def result = fsApi.updateSelectivelyDefaultValue(
        new DbApplicationFeature.Builder().valueType(FeatureValueType.STRING).build(),
        new FeatureValue().valueString("x"),
        new DbFeatureValueVersion(histId, LocalDateTime.now(), dbPerson, FeatureState.READY, "y", false, false, [], []),
        existing,
        new PersonFeaturePermission(new Person(), rolesChangeValue), false
      )
    then:
      result
      existing.defaultValue == 'x'
  }

  def "string - they pass an update the different to the historical one and historical is different existing one"() {
    when:
      def result = fsApi.updateSelectivelyDefaultValue(
        new DbApplicationFeature.Builder().valueType(FeatureValueType.STRING).build(),
        new FeatureValue().valueString("x"),
        new DbFeatureValueVersion(histId, LocalDateTime.now(), dbPerson, FeatureState.READY, "z", false, false, [], []),
        new DbFeatureValue.Builder().defaultValue("y").build(),
        new PersonFeaturePermission(new Person(), rolesChangeValue), false
      )
    then:
      thrown(OptimisticLockingException)
  }

  def "string - they pass an update the different to the historical one and historical is the same as existing one - but feature is locked"() {
    when:
      def result = fsApi.updateSelectivelyDefaultValue(
        new DbApplicationFeature.Builder().valueType(FeatureValueType.STRING).build(),
        new FeatureValue().valueString("x"),
        new DbFeatureValueVersion(histId, LocalDateTime.now(), dbPerson, FeatureState.READY, "y", false, false, [], []),
        new DbFeatureValue.Builder().defaultValue("y").locked(true).build(),
        new PersonFeaturePermission(new Person(), rolesChangeValue), false
      )
    then:
      thrown(LockedException)
  }

  def "string - they pass an update the different to the historical one and historical is the same as existing one - but has no role"() {
    when:
      def result = fsApi.updateSelectivelyDefaultValue(
        new DbApplicationFeature.Builder().valueType(FeatureValueType.STRING).build(),
        new FeatureValue().valueString("x"),
        new DbFeatureValueVersion(histId, LocalDateTime.now(), dbPerson, FeatureState.READY, "y", false, false, [], []),
        new DbFeatureValue.Builder().defaultValue("y").locked(true).build(),
        new PersonFeaturePermission(new Person(), rolesRead), true
      )
    then:
      thrown(FeatureApi.NoAppropriateRole)
  }

  /*
  class FeatureSqlApi @Inject constructor(
  private val database: Database, private val convertUtils: Conversions, private val cacheSource: CacheSource,
  private val rolloutStrategyValidator: RolloutStrategyValidator, private val strategyDiffer: StrategyDiffer
)
   */
}
