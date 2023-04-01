package io.featurehub.db.services

import io.featurehub.db.api.FeatureApi
import io.featurehub.db.api.LockedException
import io.featurehub.db.api.MultiFeatureValueUpdate
import io.featurehub.db.api.OptimisticLockingException
import io.featurehub.db.api.PersonFeaturePermission
import io.featurehub.db.api.RolloutStrategyUpdate
import io.featurehub.db.model.DbApplicationFeature
import io.featurehub.db.model.DbFeatureValue
import io.featurehub.db.model.DbFeatureValueVersion
import io.featurehub.db.model.FeatureState
import io.featurehub.mr.model.FeatureValue
import io.featurehub.mr.model.FeatureValueType
import io.featurehub.mr.model.RoleType
import io.featurehub.mr.model.RolloutStrategy
import scala.collection.mutable.MutableList

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
  MultiFeatureValueUpdate<RolloutStrategyUpdate> updateStrategies(List<RolloutStrategy> current, List<RolloutStrategy> historical, List<RolloutStrategy> updated) {
    return updateStrategies(current, historical, updated, new PersonFeaturePermission(person, defaultRoles))
  }

  MultiFeatureValueUpdate<RolloutStrategyUpdate> updateStrategies(List<RolloutStrategy> current, List<RolloutStrategy> historical, List<RolloutStrategy> updated, PersonFeaturePermission person) {
    currentFeature = new DbFeatureValue.Builder().locked(currentLock).rolloutStrategies(current).build()

    return fsApi.updateSelectivelyRolloutStrategies(
      person,
      new FeatureValue().rolloutStrategies(updated),
      new DbFeatureValueVersion(histId, LocalDateTime.now(), dbPerson, FeatureState.READY, "y", false, false, historical, [], feature),
      currentFeature, lockChanged
    )
  }

  def "when i add strategies i can"() {
    def newRolloutStrategy = new RolloutStrategy().id("x123")
    when:
      def result = updateStrategies([], [], [newRolloutStrategy])
    then:
      result.hasChanged
      result.updated == [new RolloutStrategyUpdate("add", null, newRolloutStrategy)]
      result.previous == []
      currentFeature.rolloutStrategies.find({it.id == 'x123'})
  }

  def "when i have no permissions, i can't add strategies"() {
    when:
      updateStrategies([], [], [new RolloutStrategy().id("x123")], new PersonFeaturePermission(person, testRoles))
    then:
      thrown(FeatureApi.NoAppropriateRole)
    where:
      testRoles   | _
      rolesRead   | _
      rolesLock   | _
      rolesUnlock | _
  }

  def "if i pass a strategy and the existing one has strategies, they don't get deleted"() {
    def newRolloutStrategy = new RolloutStrategy().id('2345')
    when:
      def result = updateStrategies([new RolloutStrategy().id('1234')], [], [newRolloutStrategy])
    then:
      result.hasChanged
      result.updated == [new RolloutStrategyUpdate("add", null, newRolloutStrategy)]
      result.previous == []
      currentFeature.rolloutStrategies.find({it.id == '1234'})
      currentFeature.rolloutStrategies.find({it.id == '2345'})
  }

  def "if i pass no strategies, and the existing one has strategies and the historical one matches mine, no change is detected"() {
    when:
      def result = updateStrategies([new RolloutStrategy().id('1234')], [], [])
    then:
      !result.hasChanged
  }

  def "current matches historical and i delete a strategy"() {
    given:
      def existingStrategy = new RolloutStrategy().id('1234')
      def updatedStrategy = new RolloutStrategy().id('2345')
    when:
      def result = updateStrategies([existingStrategy], [existingStrategy], [updatedStrategy])
    then:
      result.hasChanged
      result.updated == [new RolloutStrategyUpdate("add", null, updatedStrategy),
                         new RolloutStrategyUpdate("delete", existingStrategy, null)]
      result.previous == [existingStrategy]
      currentFeature.rolloutStrategies.size() == 1
      currentFeature.rolloutStrategies.find({it.id == '2345'})
  }

  def "delete historical strategy by replacement and don't have permission"() {
    when:
      def existingStrategy = new RolloutStrategy().id('1234')
      updateStrategies([existingStrategy], [existingStrategy], [new RolloutStrategy().id('2345')], new PersonFeaturePermission(person, testRoles))
    then:
      thrown(FeatureApi.NoAppropriateRole)
    where:
      testRoles   | _
      rolesRead   | _
      rolesLock   | _
      rolesUnlock | _
  }

  def "delete historical strategy by removal and don't have permission"() {
    when:
      def existingStrategy = new RolloutStrategy().id('1234')
      updateStrategies([existingStrategy], [existingStrategy], [], new PersonFeaturePermission(person, testRoles))
    then:
      thrown(FeatureApi.NoAppropriateRole)
    where:
      testRoles   | _
      rolesRead   | _
      rolesLock   | _
      rolesUnlock | _
  }

  def "reorder strategies and no permission"() {
    when:
      def existingStrategies = [new RolloutStrategy().id('1234'), new RolloutStrategy().id('23451')]
      updateStrategies(existingStrategies, existingStrategies, [existingStrategies[1], existingStrategies[0]], new PersonFeaturePermission(person, testRoles))
    then:
      thrown(FeatureApi.NoAppropriateRole)
    where:
      testRoles   | _
      rolesRead   | _
      rolesLock   | _
      rolesUnlock | _
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

    def updatedStrategy = new RolloutStrategy().id('1234').name('ex')
    def newStrategy = new RolloutStrategy().id('2345')
    def strategies = [
      updatedStrategy,
      newStrategy]
    when:
      def result = updateStrategies([existingStrategy], [existingStrategy], strategies)
    then:
      result.hasChanged
      result.updated == [new RolloutStrategyUpdate("change",existingStrategy, updatedStrategy),
      new RolloutStrategyUpdate("add", null, newStrategy)]
      result.previous == [existingStrategy]

      currentFeature.rolloutStrategies.size() == 2
      currentFeature.rolloutStrategies.findIndexOf {it.id == '1234' } == 0
      currentFeature.rolloutStrategies[0].name == 'ex'
  }

  def "replace value with invalid permissions should fail"() {
    when:
      def existingStrategy = new RolloutStrategy().id('1234')
      updateStrategies([existingStrategy], [existingStrategy], [
        new RolloutStrategy().id('1234').name('ex'),
        new RolloutStrategy().id('2345')], new PersonFeaturePermission(person, testRoles))
    then:
      thrown(FeatureApi.NoAppropriateRole)
    where:
      testRoles   | _
      rolesRead   | _
      rolesLock   | _
      rolesUnlock | _
  }

  def "add and delete at the same time from historical"() {
    def newStrategy = new RolloutStrategy().id('1111').name('sandra')
    def deletedStrategy = new RolloutStrategy().id('2343').name('mary')
    when:
      def result = updateStrategies(
        [new RolloutStrategy().id('1234').name('susan')],
            [deletedStrategy],
            [newStrategy]
      )
    then:
      result.hasChanged
      result.updated == [new RolloutStrategyUpdate("add", null, newStrategy),
       new RolloutStrategyUpdate("delete", deletedStrategy, null)]
      result.previous ==  [deletedStrategy]
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
      !result.hasChanged
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

  def "i can reorder a record when there has been another one added"() {
    given:
      def a = new RolloutStrategy().id('a').name('irina')
      def b = new RolloutStrategy().id('b').name('isaac')
      def c = new RolloutStrategy().id('c').name('seb')
    when:
      def current = [a, b, c]
      def newWorldOrder = [b, a]
      def historical = [a, b]
      def result = updateStrategies(current, historical, newWorldOrder)
    then:
      result
      currentFeature.rolloutStrategies[0] == b
      currentFeature.rolloutStrategies[1] == a
      currentFeature.rolloutStrategies[2] == c
  }

  def "i can reorder a record when one has been deleted"() {
    given:
      def a = new RolloutStrategy().id('a').name('irina')
      def b = new RolloutStrategy().id('b').name('isaac')
      def c = new RolloutStrategy().id('c').name('seb')
    when:
      def current = [c, b]
      def newWorldOrder = [b, a]
      def historical = [a, b]
      def result = updateStrategies(current, historical, newWorldOrder)
    then:
      result
      currentFeature.rolloutStrategies[0] == b
      currentFeature.rolloutStrategies[1] == c
  }
}
