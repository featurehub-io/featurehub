package io.featurehub.db.services

import io.ebean.Database
import io.featurehub.db.api.RolloutStrategyValidator
import io.featurehub.db.model.DbFeatureValueVersionKey
import io.featurehub.db.model.DbPerson
import io.featurehub.db.services.strategies.StrategyDiffer
import io.featurehub.mr.events.common.CacheSource
import io.featurehub.mr.model.Person
import io.featurehub.mr.model.RoleType
import spock.lang.Specification

class FeatureAuditingBaseUnitSpec extends Specification {
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
  final rolesLock = [RoleType.LOCK] as Set<RoleType>
  final rolesUnlock = [RoleType.UNLOCK] as Set<RoleType>
  final rolesRead = [RoleType.READ] as Set<RoleType>
}
