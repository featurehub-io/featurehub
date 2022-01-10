package io.featurehub.edge.db.sql

import io.ebean.annotation.Transactional
import io.featurehub.dacha.api.DachaApiKeyService
import io.featurehub.dacha.model.*
import io.featurehub.db.model.DbApplicationFeature
import io.featurehub.db.model.DbFeatureValue
import io.featurehub.db.model.query.QDbApplicationFeature
import io.featurehub.db.model.query.QDbFeatureValue
import io.featurehub.db.model.query.QDbServiceAccountEnvironment
import io.featurehub.mr.model.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*
import java.util.stream.Collectors

class DbDachaSqlApi : DachaApiKeyService {
  private val log: Logger = LoggerFactory.getLogger(DbDachaSqlApi::class.java)

  // these are not actually required because the stats API isn't used, so we make them up
  private val fakeApplicationId = UUID.randomUUID()
  private val fakePortfolioId = UUID.randomUUID()
  private val fakeOrganisationId = UUID.randomUUID()

  @Transactional(readOnly = true)
  override fun getApiKeyDetails(eId: UUID, serviceAccountKey: String): DachaKeyDetailsResponse? {
    val saEnv = findMatch(
      eId,
      serviceAccountKey
    ).environment.parentApplication.features.fetch(
      QDbApplicationFeature.Alias.key,
      QDbApplicationFeature.Alias.id,
      QDbApplicationFeature.Alias.valueType,
    )
      .environment.environmentFeatures.fetch(
        QDbFeatureValue.Alias.locked,
        QDbFeatureValue.Alias.version,
        QDbFeatureValue.Alias.rolloutStrategies,
        QDbFeatureValue.Alias.defaultValue,
      )
      .findOne()

    return if (saEnv != null) {
      val featureValues = saEnv.environment.environmentFeatures.map { it.feature.key to it }.toMap()
      val features = saEnv.environment.parentApplication.features
      val response = DachaKeyDetailsResponse()
        .serviceKeyId(saEnv.serviceAccount.id)
        .applicationId(fakeApplicationId)
        .portfolioId(fakePortfolioId)
        .organizationId(fakeOrganisationId)
        .features(features.map { toFeatureValueCacheItem(it, featureValues[it.key]) })

      response.etag = calculateEtag(response)
      log.trace("etag is {}", response.etag)

      response
    } else {
      null
    }
  }

  private fun findMatch(
    eId: UUID,
    serviceAccountKey: String
  ): QDbServiceAccountEnvironment {
    val q = QDbServiceAccountEnvironment()
      .select(QDbServiceAccountEnvironment.Alias.serviceAccount.id)
      .environment.id.eq(eId)

    return if (serviceAccountKey.contains("*")) {
      q.serviceAccount.apiKeyClientEval.eq(serviceAccountKey)
    } else {
      q.serviceAccount.apiKeyServerEval.eq(serviceAccountKey)
    }
  }

  private fun toFeatureValueCacheItem(
    feature: DbApplicationFeature,
    fv: DbFeatureValue?
  ): CacheEnvironmentFeature =
    CacheEnvironmentFeature()
      .feature(CacheFeature().key(feature.key).id(feature.id).valueType(feature.valueType))
      .value(if (fv == null) toEmptyFeatureValue(feature) else toFeatureValue(fv))


  private fun toEmptyFeatureValue(feature: DbApplicationFeature): CacheFeatureValue =
    CacheFeatureValue()
      .key(feature.key)
      .id(feature.id)
      .version(0)
      .locked(false)

  private fun toFeatureValue(dbFeature: DbFeatureValue): CacheFeatureValue {
    val fv = CacheFeatureValue()
      .key(dbFeature.feature.key)
      .locked(dbFeature.isLocked)
      .version(dbFeature.version)
      .id(dbFeature.id)

    // we haven't implemented shared rollout strategies so don't both to include those
    when (dbFeature.feature.valueType) {
      FeatureValueType.BOOLEAN -> fv.value(dbFeature.defaultValue?.toBoolean())
      FeatureValueType.STRING, FeatureValueType.JSON -> fv.value(dbFeature.defaultValue)
      FeatureValueType.NUMBER -> fv.value(dbFeature.defaultValue?.toBigDecimal())
      else -> fv.value(null)
    }

    if (dbFeature.rolloutStrategies != null) {
      fv.rolloutStrategies(dbFeature.rolloutStrategies.map { fromRolloutStrategy(it) })
    } else {
      fv.rolloutStrategies(listOf())
    }


    return fv
  }

  private fun fromRolloutStrategy(rs: RolloutStrategy): CacheRolloutStrategy {
    return CacheRolloutStrategy()
      .id(rs.id ?: "rs-id")
      .percentage(rs.percentage)
      .percentageAttributes(rs.percentageAttributes)
      .value(rs.value)
      .attributes(if (rs.attributes == null) ArrayList() else rs.attributes!!
        .stream().map { rsa: RolloutStrategyAttribute -> fromRolloutStrategyAttribute(rsa) }
        .collect(Collectors.toList()))
  }

  private fun fromRolloutStrategyAttribute(rsa: RolloutStrategyAttribute): CacheRolloutStrategyAttribute {
    return CacheRolloutStrategyAttribute()
      .conditional(rsa.conditional!!)
      .values(rsa.values)
      .fieldName(rsa.fieldName!!)
      .type(rsa.type!!)
  }

  private fun calculateEtag(details: DachaKeyDetailsResponse): String {
    val det =
      details
        .features!!.map { fvci -> fvci.feature.id.toString() + "-" + (fvci.value?.version ?: "0000") }
        .joinToString("-")
    return Integer.toHexString(det.hashCode())
  }


  @Transactional(readOnly = true)
  override fun getApiKeyPermissions(
    eId: UUID,
    serviceAccountKey: String,
    featureKey: String
  ): DachaPermissionResponse? {
    // we need to find the api key and its permissions first and foremost
    val sa = QDbServiceAccountEnvironment()
      .select(
        QDbServiceAccountEnvironment.Alias.serviceAccount.id,
        QDbServiceAccountEnvironment.Alias.permissions,
      )
      .environment.id.eq(eId)

    val serviceAccount = if (serviceAccountKey.contains("*")) {
      sa.serviceAccount.apiKeyClientEval.eq(serviceAccountKey)
    } else {
      sa.serviceAccount.apiKeyServerEval.eq(serviceAccountKey)
    }.findOne() ?: return null

    val applicationFeature = QDbApplicationFeature()
      .select(
        QDbApplicationFeature.Alias.key,
        QDbApplicationFeature.Alias.id,
        QDbApplicationFeature.Alias.valueType,
        QDbApplicationFeature.Alias.parentApplication.id,
      )
      .parentApplication.environments.id.eq(eId)
      .key.eq(featureKey)
      .environmentFeatures.fetch(
        QDbFeatureValue.Alias.locked,
        QDbFeatureValue.Alias.version,
        QDbFeatureValue.Alias.rolloutStrategies,
        QDbFeatureValue.Alias.defaultValue,
        QDbFeatureValue.Alias.environment.id
      )

    val feature =
      applicationFeature.parentApplication.environments.serviceAccountEnvironments.eq(serviceAccount).findOne() ?: return null

    val foundFeatureValue = feature.environmentFeatures.find { it.environment.id == eId }
    val featureValue = if (foundFeatureValue == null) toEmptyFeatureValue(feature).id(feature.id) else toFeatureValue(foundFeatureValue)

    // it wants roles, valueType, key, locked, the feature value
    return DachaPermissionResponse()
      .feature(
        CacheEnvironmentFeature()
          .feature(CacheFeature().key(featureKey).id(feature.id).valueType(feature.valueType))
          .value(featureValue)
      )
      .roles(serviceAccount.permissions?.split(",")?.filterNot { it.isEmpty() }?.map { RoleType.valueOf(it) }
        ?: listOf())
      .serviceKeyId(serviceAccount.serviceAccount.id)
      .applicationId(feature.parentApplication.id)
      .portfolioId(fakePortfolioId)
      .organizationId(fakeOrganisationId)
  }
}
