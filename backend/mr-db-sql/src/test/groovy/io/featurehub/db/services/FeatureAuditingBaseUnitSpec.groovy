package io.featurehub.db.services

import io.ebean.Database
import io.featurehub.db.api.RolloutStrategyValidator
import io.featurehub.db.model.DbFeatureValueVersionKey
import io.featurehub.db.model.DbPerson
import io.featurehub.mr.events.common.CacheSource
import io.featurehub.mr.events.common.FeatureMessagingCloudEventPublisher
import io.featurehub.mr.model.Person
import io.featurehub.mr.model.RoleType
import io.featurehub.utils.ExecutorSupplier
import spock.lang.Specification

import java.util.concurrent.ExecutorService

class FeatureAuditingBaseUnitSpec extends Specification {
  Database database
  Conversions conversions
  CacheSource cacheSource
  RolloutStrategyValidator rolloutStrategyValidator
  FeatureMessagingCloudEventPublisher featureMessagingCloudEventPublisher
  ExecutorSupplier executorSupplier
  int threadPoolSize
  ExecutorService executor

  FeatureSqlApi fsApi
  Person person
  DbPerson dbPerson
  DbFeatureValueVersionKey histId
  def setup() {
    database = Mock()
    conversions = Mock()
    cacheSource = Mock()
    rolloutStrategyValidator = Mock()
    person = new Person()
    dbPerson = new DbPerson.Builder().build()

    histId = new DbFeatureValueVersionKey(UUID.randomUUID(), 1)
    featureMessagingCloudEventPublisher = Mock()
    executorSupplier = Mock()
    executor = Mock()
    fsApi = new FeatureSqlApi(database, conversions, cacheSource, rolloutStrategyValidator, featureMessagingCloudEventPublisher, executorSupplier)
  }

  static final rolesChangeValue = [RoleType.CHANGE_VALUE] as Set<RoleType>
  static final rolesLock = [RoleType.LOCK] as Set<RoleType>
  static final rolesUnlock = [RoleType.UNLOCK] as Set<RoleType>
  static final rolesRead = [RoleType.READ] as Set<RoleType>
}
