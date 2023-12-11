package io.featurehub.db.services

import io.ebean.Database
import io.featurehub.db.api.RolloutStrategyValidator
import io.featurehub.db.model.DbApplication
import io.featurehub.db.model.DbApplicationFeature
import io.featurehub.db.model.DbEnvironment
import io.featurehub.db.model.DbFeatureValue
import io.featurehub.db.model.DbFeatureValueVersionKey
import io.featurehub.db.model.DbPerson
import io.featurehub.db.publish.CacheSourceFeatureGroupApi
import io.featurehub.messaging.converter.FeatureMessagingConverter
import io.featurehub.mr.events.common.CacheSource
import io.featurehub.mr.model.FeatureValueType
import io.featurehub.mr.model.Person
import io.featurehub.mr.model.RoleType
import spock.lang.Specification

class FeatureAuditingBaseUnitSpec extends Specification {
  Database database
  Conversions conversions
  CacheSource cacheSource
  RolloutStrategyValidator rolloutStrategyValidator
  FeatureMessagingConverter featureMessagingCloudEventPublisher

  FeatureSqlApi fsApi
  Person person
  DbPerson dbPerson
  DbFeatureValueVersionKey histId
  DbEnvironment environment
  DbApplication app


  def setup() {
    database = Mock()
    conversions = Mock()
    cacheSource = Mock()
    rolloutStrategyValidator = Mock()
    person = new Person()
    dbPerson = new DbPerson.Builder().build()
    environment = new DbEnvironment.Builder().name("fake").build()

    histId = new DbFeatureValueVersionKey(UUID.randomUUID(), 1)
    featureMessagingCloudEventPublisher = Mock()

    fsApi =new FeatureSqlApi(conversions, cacheSource, rolloutStrategyValidator, featureMessagingCloudEventPublisher, Mock(CacheSourceFeatureGroupApi))

    app = new DbApplication()
  }

  static final rolesChangeValue = [RoleType.CHANGE_VALUE] as Set<RoleType>
  static final rolesLock = [RoleType.LOCK] as Set<RoleType>
  static final rolesUnlock = [RoleType.UNLOCK] as Set<RoleType>
  static final rolesRead = [RoleType.READ] as Set<RoleType>

  DbApplicationFeature af(FeatureValueType type = FeatureValueType.BOOLEAN) {
    return new DbApplicationFeature.Builder().parentApplication(app).key('fred').name('choochoo').valueType(type).build()
  }

  DbFeatureValue featureValue(String val, DbApplicationFeature feat) {
    return new DbFeatureValue(dbPerson, false, feat, environment, val)
  }
}
