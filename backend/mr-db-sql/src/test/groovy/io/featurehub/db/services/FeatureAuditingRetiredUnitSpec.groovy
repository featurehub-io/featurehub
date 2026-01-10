package io.featurehub.db.services

import io.featurehub.db.api.FeatureApi
import io.featurehub.db.api.FeatureApi.LockedException
import io.featurehub.db.api.PersonFeaturePermission
import io.featurehub.db.api.SingleFeatureValueUpdate
import io.featurehub.db.model.DbApplicationFeature
import io.featurehub.db.model.DbFeatureValueVersion
import io.featurehub.mr.model.FeatureValue
import io.featurehub.mr.model.FeatureValueType
import io.featurehub.mr.model.RoleType

import java.time.LocalDateTime

class FeatureAuditingRetiredUnitSpec extends FeatureAuditingBaseUnitSpec {
  Set<RoleType> roles
  boolean featureLocked
  boolean changingLocked
  DbApplicationFeature feature

  def setup() {
    roles = rolesChangeValue
    featureLocked = false
    changingLocked = false
    feature = af()
  }

  SingleFeatureValueUpdate<Boolean> update(Boolean currentRetired, boolean historicalRetired, boolean changingRetired) {
    return updateFeatureApi.updateSelectivelyRetired(
      new PersonFeaturePermission(person, roles),
      new FeatureValue().retired(changingRetired),
      new DbFeatureValueVersion(histId, LocalDateTime.now(), dbPerson, "y", featureLocked, historicalRetired, [], [], feature, 0),
      featureValue("y", feature).with {
        it.locked = featureLocked
        it.retired = currentRetired
        it
      },
      changingLocked
    )
  }

  def "if the existing retired is null and the new retired is set to true, it will update"() {
    when:
      def result = update(null, false, true)
    then:
      result
  }

  def "i cannot update a locked retired setting"() {
    given:
      featureLocked = true
    when:
      update(false, false, true)
    then:
      thrown(LockedException)
  }

  def "i cannot update the retired setting if i don't have the permissions"() {
    given:
      roles = rolesRead
    when:
      update(false, false, true)
    then:
      thrown(FeatureApi.NoAppropriateRole)
  }

  def "updating it its existing value results in no change"() {
    when:
      def result = update(false, true, false)
    then:
      !result.hasChanged
  }

  def "updating it its historical value results in no change"() {
    when:
      def result = update(false, true, true)
    then:
      !result.hasChanged
  }

  def "updating from its historical and existing value with the right permissions is ok"() {
    when:
      def result = update(false, false, true)
    then:
      result.hasChanged
      result.updated
      !result.previous
  }
}
