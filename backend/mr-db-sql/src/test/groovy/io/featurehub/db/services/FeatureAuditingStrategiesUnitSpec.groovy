package io.featurehub.db.services

import io.featurehub.db.api.LockedException
import io.featurehub.db.api.OptimisticLockingException
import io.featurehub.db.api.PersonFeaturePermission
import io.featurehub.db.model.DbApplicationFeature
import io.featurehub.db.model.DbFeatureValue
import io.featurehub.db.model.DbFeatureValueVersion
import io.featurehub.db.model.FeatureState
import io.featurehub.mr.model.FeatureValue
import io.featurehub.mr.model.FeatureValueType
import io.featurehub.mr.model.RoleType
import io.featurehub.mr.model.RolloutStrategy

import java.time.LocalDateTime

class FeatureAuditingStrategiesUnitSpec extends FeatureAuditingBaseUnitSpec {
  DbFeatureValue currentFeature
  DbApplicationFeature feature
  boolean lockChanged
  Set<RoleType> defaultRoles
  boolean currentLock

  def setup() {
    lockChanged = false
    currentLock = false
    defaultRoles = rolesChangeValue
    feature = new DbApplicationFeature.Builder().valueType(FeatureValueType.BOOLEAN).build()
  }

  /**
   *
   * @param current - these are the strategies in the database now
   * @param historical - these are the ones as at the time we were updating
   * @param updated - these are the strategies we are laying on top of historical to be applied to current
   * @return - current should be updated with the results of updated
   */
  boolean updateStrategies(List<RolloutStrategy> current, List<RolloutStrategy> historical, List<RolloutStrategy> updated) {
    currentFeature = new DbFeatureValue.Builder().locked(currentLock).rolloutStrategies(current).build()

    return fsApi.updateSelectivelyRolloutStrategies(
      new PersonFeaturePermission(person, defaultRoles),
      new FeatureValue().rolloutStrategies(updated),
      new DbFeatureValueVersion(histId, LocalDateTime.now(), dbPerson, FeatureState.READY, "y", false, false, historical, [], feature),
      currentFeature, lockChanged
    )
  }

  def "when i add strategies i can"() {
    when:
      def result = updateStrategies([], [], [new RolloutStrategy().id("x123")])
    then:
      result
      currentFeature.rolloutStrategies.find({it.id == 'x123'})
  }

  def "if i pass a strategy and the existing one has strategies, they don't get deleted"() {
    when:
      def result = updateStrategies([new RolloutStrategy().id('1234')], [], [new RolloutStrategy().id('2345')])
    then:
      result
      currentFeature.rolloutStrategies.find({it.id == '1234'})
      currentFeature.rolloutStrategies.find({it.id == '2345'})
  }

  def "if i pass no strategies, and the existing one has strategies and the historical one matches mine, no change is detected"() {
    when:
      def result = updateStrategies([new RolloutStrategy().id('1234')], [], [])
    then:
      !result
  }

  def "current matches historical and i delete a strategy"() {
    given:
      def existingStrategy = new RolloutStrategy().id('1234')
    when:
      def result = updateStrategies([existingStrategy], [existingStrategy], [new RolloutStrategy().id('2345')])
    then:
      result
      currentFeature.rolloutStrategies.size() == 1
      currentFeature.rolloutStrategies.find({it.id == '2345'})
  }

  def "current and historical do not match the same strategy id and we change it"() {
    when:
      updateStrategies([new RolloutStrategy().id('1234').name('fred')],
        [new RolloutStrategy().id('1234').name('mary')],
        [new RolloutStrategy().id('1234').name('papa joe')])
    then:
      thrown(OptimisticLockingException)
  }

  def "current and historical are the same and we want to change it, should be in exactly the same location and updated"() {
    given:
      def existingStrategy = new RolloutStrategy().id('1234')
    when:
      def result = updateStrategies([existingStrategy], [existingStrategy], [
        new RolloutStrategy().id('1234').name('ex'),
        new RolloutStrategy().id('2345')])
    then:
      result
      currentFeature.rolloutStrategies.size() == 2
      currentFeature.rolloutStrategies.findIndexOf {it.id == '1234' } == 0
      currentFeature.rolloutStrategies[0].name == 'ex'
  }

  def "add and delete at the same time from historical"() {
    when:
      def result = updateStrategies([new RolloutStrategy().id('1234').name('susan')],
            [new RolloutStrategy().id('2343').name('mary')],
            [new RolloutStrategy().id('1111').name('sandra')]
      )
    then:
      result
      currentFeature.rolloutStrategies.size() == 2
      currentFeature.rolloutStrategies.find{ it.id == '1234' }?.name == 'susan'
      currentFeature.rolloutStrategies.find{ it.id == '1111' }?.name == 'sandra'
  }

  def "add and delete at the same time from historical and current"() {
    when:
      def result = updateStrategies([new RolloutStrategy().id('1234').name('susan'), new RolloutStrategy().id('2222').name('wilbur')],
            [new RolloutStrategy().id('2343').name('mary'), new RolloutStrategy().id('2222').name('wilbur')],
            [new RolloutStrategy().id('1111').name('sandra')]
      )
    then:
      result
      currentFeature.rolloutStrategies.size() == 2
      currentFeature.rolloutStrategies.find{ it.id == '1234' }?.name == 'susan'
      currentFeature.rolloutStrategies.find{ it.id == '1111' }?.name == 'sandra'
  }

  def "i add a strategy but it is locked and lock hasn't changed"() {
    given:
      currentLock = true
    when:
      updateStrategies([], [], [new RolloutStrategy().id('1234')])
    then:
      thrown(LockedException)
  }

  def "i add a strategy but it is locked and lock has changed"() {
    given:
      currentLock = true
      lockChanged = true
    when:
      def result = updateStrategies([], [], [new RolloutStrategy().id('1234')])
    then:
      result
  }

  def "when I pass in a strategy update that contains historical strategies that have been deleted, these don't get re-added"() {
    given:
      def deleted = new RolloutStrategy().id('dele').name('deleted')
      def editing = new RolloutStrategy().id('edit').name('susan')
    when:
      def result = updateStrategies([new RolloutStrategy().id('edit').name('susan')],
        [editing, deleted],
        [new RolloutStrategy().id('edit').name('susan2'), deleted]
      )
    then:
      result
      currentFeature.rolloutStrategies.size() == 1
      currentFeature.rolloutStrategies[0].name == 'susan2'
  }

  def "when I delete one that has already been deleted, that is ok"() {
    when:
      def result = updateStrategies([new RolloutStrategy().id('edit').name('susan')],
        [new RolloutStrategy().id('edit').name('susan'), new RolloutStrategy().id('dele').name('susan')],
        [new RolloutStrategy().id('edit').name('susan')]
      )
    then:
      !result
  }

  def "when I change one that has been deleted, thats a optimistic locking issue"() {
    when:
      def result = updateStrategies([new RolloutStrategy().id('edit').name('susan')],
        [new RolloutStrategy().id('edit').name('susan'), new RolloutStrategy().id('dele').name('susan')],
        [new RolloutStrategy().id('edit').name('susan'), new RolloutStrategy().id('dele').name('susan2')]
      )
    then:
      thrown(OptimisticLockingException)
  }

  def "i can reorder the strategies even if i haven't changed them"() {
    given:
      def a = new RolloutStrategy().id('a').name('irina')
      def b = new RolloutStrategy().id('b').name('isaac')
      def c = new RolloutStrategy().id('c').name('seb')
    when:
      def oldOrder = [a, b, c]
      def newWorldOrder = [c, b, a]
      def result = updateStrategies(oldOrder, oldOrder, newWorldOrder)
    then:
      result
      currentFeature.rolloutStrategies[0] == c
      currentFeature.rolloutStrategies[1] == b
      currentFeature.rolloutStrategies[2] == a
  }
}
