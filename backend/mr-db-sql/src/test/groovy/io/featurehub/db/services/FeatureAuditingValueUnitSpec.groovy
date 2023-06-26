package io.featurehub.db.services


import io.featurehub.db.api.FeatureApi
import io.featurehub.db.api.FeatureApi.LockedException
import io.featurehub.db.api.OptimisticLockingException
import io.featurehub.db.api.PersonFeaturePermission
import io.featurehub.db.model.DbApplicationFeature
import io.featurehub.db.model.DbFeatureValue
import io.featurehub.db.model.DbFeatureValueVersion
import io.featurehub.db.model.FeatureState
import io.featurehub.mr.model.FeatureValue
import io.featurehub.mr.model.FeatureValueType
import io.featurehub.mr.model.Person

import java.time.LocalDateTime

/**
 * we are testing the methods internally in isolation
 */
class FeatureAuditingValueUnitSpec extends FeatureAuditingBaseUnitSpec {


  /*
   * These are the tests around the changing of the value
   */

  def "bool - they pass an update the same as the existing one but different from the historical one"() {
    given:
      def feat = af()
    when:
      def result = fsApi.updateSelectivelyDefaultValue(
        feat,
        new FeatureValue().valueBoolean(true),
        new DbFeatureValueVersion(histId, LocalDateTime.now(), dbPerson, "false", false, false, [], [], feat, 0),
        featureValue("true", feat),
        new PersonFeaturePermission(new Person(), rolesChangeValue), false
      )
    then:
      !result.hasChanged
  }

  def "bool - they pass an update the same as the historical one but different from the existing one"() {
    given:
      def feat = af()
    when:
      def result = fsApi.updateSelectivelyDefaultValue(
        feat,
        new FeatureValue().valueBoolean(false),
        new DbFeatureValueVersion(histId, LocalDateTime.now(), dbPerson, "false", false, false, [], [], feat, 0),
        featureValue("true", feat),
        new PersonFeaturePermission(new Person(), rolesChangeValue), false
      )
    then:
      !result.hasChanged
  }

  def "string - they pass an update the same as the historical one but different from the existing one"() {
    given:
      def feat = af(FeatureValueType.STRING)
    when:
      def result = fsApi.updateSelectivelyDefaultValue(
        feat,
        new FeatureValue().valueString("x"),
        new DbFeatureValueVersion(histId, LocalDateTime.now(), dbPerson, "x", false, false, [], [], feat, 0),
        featureValue("y", feat),
        new PersonFeaturePermission(new Person(), rolesChangeValue), false
      )
    then:
      !result.hasChanged
  }

  def "string - they pass an update the different to the historical one and historical is the same as existing one"() {
    given:
      def historicalValue = "y"
      def feat = af(FeatureValueType.STRING)
      def existing = featureValue(historicalValue, feat)

    def newFeatureValue = "x"
    when:
      def result = fsApi.updateSelectivelyDefaultValue(
        feat,
        new FeatureValue().valueString(newFeatureValue),
        new DbFeatureValueVersion(histId, LocalDateTime.now(), dbPerson, historicalValue, false, false, [], [], feat, 0),
        existing,
        new PersonFeaturePermission(new Person(), rolesChangeValue), false
      )
    then:
      result.hasChanged
      result.updated == newFeatureValue
      result.previous == historicalValue
      existing.defaultValue == 'x'
  }

  def "string - they pass an update the different to the historical one and historical is different existing one"() {
    given:
      def feat = af(FeatureValueType.STRING)
    when:
      def result = fsApi.updateSelectivelyDefaultValue(
        feat,
        new FeatureValue().valueString("x"),
        new DbFeatureValueVersion(histId, LocalDateTime.now(), dbPerson, "z", false, false, [], [], feat, 0),
        featureValue("y", feat),
        new PersonFeaturePermission(new Person(), rolesChangeValue), false
      )
    then:
      thrown(OptimisticLockingException)
  }

  def "string - they pass an update the different to the historical one and historical is the same as existing one - but feature is locked"() {
    given:
      def feat = af(FeatureValueType.STRING)
    when:
      def result = fsApi.updateSelectivelyDefaultValue(
        feat,
        new FeatureValue().valueString("x"),
        new DbFeatureValueVersion(histId, LocalDateTime.now(), dbPerson, "y", false, false, [], [], feat, 0),
        featureValue("y", feat).with { it.locked = true; it },
        new PersonFeaturePermission(new Person(), rolesChangeValue), false
      )
    then:
      thrown(LockedException)
  }

  def "string - they pass an update the different to the historical one and historical is the same as existing one - but has no role"() {
    given:
      def feat = af(FeatureValueType.STRING)
    when:
      def result = fsApi.updateSelectivelyDefaultValue(
        feat,
        new FeatureValue().valueString("x"),
        new DbFeatureValueVersion(histId, LocalDateTime.now(), dbPerson, "y", false, false, [], [], feat, 0),
        featureValue("y", feat).with { it.locked = true; it },
        new PersonFeaturePermission(new Person(), rolesRead), true
      )
    then:
      thrown(FeatureApi.NoAppropriateRole)
  }

}
