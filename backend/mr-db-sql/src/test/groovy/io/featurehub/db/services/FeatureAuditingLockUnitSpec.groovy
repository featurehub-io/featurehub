package io.featurehub.db.services

import io.featurehub.db.api.FeatureApi
import io.featurehub.db.api.PersonFeaturePermission
import io.featurehub.db.api.SingleFeatureValueUpdate
import io.featurehub.db.model.DbApplicationFeature
import io.featurehub.db.model.DbFeatureValue
import io.featurehub.db.model.DbFeatureValueVersion
import io.featurehub.db.model.FeatureState
import io.featurehub.mr.model.FeatureValue
import io.featurehub.mr.model.FeatureValueType
import io.featurehub.mr.model.Person
import io.featurehub.mr.model.RoleType

import java.time.LocalDateTime

class FeatureAuditingLockUnitSpec extends FeatureAuditingBaseUnitSpec {
  DbApplicationFeature feature

  def setup() {
    feature = af()
  }

  SingleFeatureValueUpdate<Boolean> locked(boolean current, boolean historical, boolean changing) {
    return locked(current, historical, changing, rolesRead)
  }

  SingleFeatureValueUpdate<Boolean> locked(boolean current, boolean historical, boolean changing, Set<RoleType> roles) {
    return updateFeatureApi.updateSelectivelyLocked(
      new FeatureValue().locked(changing),
      new DbFeatureValueVersion(histId, LocalDateTime.now(), dbPerson, "y", historical, false, [], [], feature, 0),
      featureValue("y", feature).with { it.locked = current; it },
      new PersonFeaturePermission(new Person(), roles)
    )
  }

  /*
   * these are the tests around the changing of locked
   */
  def "lock: no change from historical or current"() {
    when:
      def result = locked(true, true, true)
    then:
      !result.hasChanged
  }

  def "lock: current not locked, historical, changed locked - so not trying to change locking status"() {
    when:
      def result = locked(false, true, true)
    then:
      !result.hasChanged
  }

  def "lock: current is locked, historical not locked, changed locked - so trying to change the lock status to what it is"() {
    when:
      def result = locked(true, false, true)
    then:
      !result.hasChanged
  }

  def "lock: current is not locked, historical is not locked, changed to locked - no permission"() {
    when:
      def result = locked(false, false, true)
    then:
      thrown(FeatureApi.NoAppropriateRole)
  }

  def "lock: current is not locked, historical is not locked, changed to locked - unlock role, no permission"() {
    when:
      def result = locked(false, false, true, rolesUnlock)
    then:
      thrown(FeatureApi.NoAppropriateRole)
  }

  def "lock: current is not locked, historical is not locked, changed to locked - lock role, has permission"() {
    when:
      def result = locked(false, false, true, rolesLock)
    then:
      result.hasChanged
      result.updated
      !result.previous
  }

  def "unlock"() {
    when:
      def result = locked(true, true, false, rolesUnlock)
    then:
      result.hasChanged
      !result.updated
      result.previous
  }
}
