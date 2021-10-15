package io.featurehub.edge.db.sql

import io.ebean.annotation.Transactional
import io.featurehub.dacha.api.DachaApiKeyService
import io.featurehub.db.model.DbApplicationFeature
import io.featurehub.db.model.DbFeatureValue
import io.featurehub.db.model.query.QDbApplicationFeature
import io.featurehub.db.model.query.QDbFeatureValue
import io.featurehub.db.model.query.QDbServiceAccountEnvironment
import io.featurehub.mr.model.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.math.BigDecimal
import java.util.*

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
        .features(features.map { toFeatureValueCacheItem(eId, it, featureValues[it.key]) })

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

  private fun toFeatureValueCacheItem(envId: UUID, feature: DbApplicationFeature, fv: DbFeatureValue?) : FeatureValueCacheItem =
    FeatureValueCacheItem()
      .environmentId(envId)
      .feature(Feature().key(feature.key).id(feature.id).valueType(feature.valueType))
      .value(if (fv == null) toEmptyFeatureValue(feature) else toFeatureValue(fv) )


  private fun toEmptyFeatureValue(feature: DbApplicationFeature): FeatureValue =
    FeatureValue()
      .key(feature.key)
      .id(feature.id)
      .version(0)
      .locked(false)

  private fun toFeature(dbFeature: DbFeatureValue) =
    Feature().key(dbFeature.feature.key).id(dbFeature.feature.id).valueType(dbFeature.feature.valueType)

  private fun toFeatureValue(dbFeature: DbFeatureValue): FeatureValue {
    val fv = FeatureValue()
      .key(dbFeature.feature.key)
      .locked(dbFeature.isLocked)
      .version(dbFeature.version)
      .id(dbFeature.id)

    // we haven't implemented shared rollout strategies so don't both to include those
    when (dbFeature.feature.valueType) {
      FeatureValueType.BOOLEAN -> fv.valueBoolean(dbFeature.defaultValue == "true")
      FeatureValueType.STRING -> fv.valueString(dbFeature.defaultValue)
      FeatureValueType.NUMBER -> if (dbFeature.defaultValue != null) fv.valueNumber(BigDecimal(dbFeature.defaultValue))
      FeatureValueType.JSON -> fv.valueJson(dbFeature.defaultValue)
      else -> fv.valueString(null)
    }

    return fv
  }

  private fun calculateEtag(details: DachaKeyDetailsResponse): String {
    val det =
      details
        .features!!.map { fvci -> fvci.feature.id.toString() + "-" + fvci.value.version }
        .joinToString("-")
    return Integer.toHexString(det.hashCode())
  }


  @Transactional(readOnly = true)
  override fun getApiKeyPermissions(
    eId: UUID,
    serviceAccountKey: String,
    featureKey: String
  ): DachaPermissionResponse? {
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

    val q = QDbFeatureValue()
      .select(
        QDbFeatureValue.Alias.locked,
        QDbFeatureValue.Alias.version,
        QDbFeatureValue.Alias.rolloutStrategies,
        QDbFeatureValue.Alias.defaultValue,
      )
      .environment.id.eq(eId).feature.key.eq(featureKey).feature.fetch(
        QDbApplicationFeature.Alias.key,
        QDbApplicationFeature.Alias.id,
        QDbApplicationFeature.Alias.valueType,
        )

    val found = if (serviceAccountKey.contains("*")) {
      q.environment.serviceAccountEnvironments.eq(serviceAccount)
    } else {
      q.environment.serviceAccountEnvironments.eq(serviceAccount)
    }.findOne() ?: return null

    // it wants roles, valueType, key, locked, the feature value
    return DachaPermissionResponse()
      .feature(
        DachaFeatureValueItem()
          .feature(toFeature(found))
          .value(toFeatureValue(found))
      )
      .roles(serviceAccount.permissions?.split(",")?.filterNot { it.isEmpty() }?.map { RoleType.valueOf(it) } ?: listOf())
      .serviceKeyId(serviceAccount.serviceAccount.id)
      .applicationId(fakeApplicationId)
      .portfolioId(fakePortfolioId)
      .organizationId(fakeOrganisationId)
  }
}
