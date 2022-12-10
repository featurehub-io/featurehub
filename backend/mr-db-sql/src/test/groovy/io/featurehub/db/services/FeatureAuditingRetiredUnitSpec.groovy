package io.featurehub.db.services

import io.featurehub.db.api.FeatureApi
import io.featurehub.db.api.LockedException
import io.featurehub.db.api.PersonFeaturePermission
import io.featurehub.db.model.DbApplicationFeature
import io.featurehub.db.model.DbFeatureValue
import io.featurehub.db.model.DbFeatureValueVersion
import io.featurehub.db.model.FeatureState
import io.featurehub.mr.model.FeatureValue
import io.featurehub.mr.model.FeatureValueType
import io.featurehub.mr.model.RoleType

import java.time.LocalDateTime

class FeatureAuditingRetiredUnitSpec extends FeatureAuditingBaseUnitSpec {
  Set<RoleType> roles
  boolean locked
  boolean changingLocked
  DbApplicationFeature feature

  def setup() {
    roles = rolesChangeValue
    locked = false
    changingLocked = false
    feature = new DbApplicationFeature.Builder().valueType(FeatureValueType.BOOLEAN).build()
  }

  boolean update(Boolean currentRetired, boolean historicalRetired, boolean changingRetired) {
    return fsApi.updateSelectivelyRetired(
      new PersonFeaturePermission(person, roles),
      new FeatureValue().retired(changingRetired),
      new DbFeatureValueVersion(histId, LocalDateTime.now(), dbPerson, FeatureState.READY, "y", locked, historicalRetired, [], [], feature),
      new DbFeatureValue.Builder().retired(currentRetired).locked(locked).build(),
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
      locked = true
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
      !result
  }

  def "updating it its historical value results in no change"() {
    when:
      def result = update(false, true, true)
    then:
      !result
  }

  def "updating from its historical and existing value with the right permissions is ok"() {
    when:
      def result = update(false, false, true)
    then:
      result
  }
}
