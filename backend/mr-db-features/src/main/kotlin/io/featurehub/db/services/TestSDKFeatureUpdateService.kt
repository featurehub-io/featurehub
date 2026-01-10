package io.featurehub.db.services

import io.featurehub.db.api.PersonFeaturePermission
import io.featurehub.db.api.RolloutStrategyValidator
import io.featurehub.db.listener.FeatureUpdateBySDKApi
import io.featurehub.db.model.query.QDbApplicationFeature
import io.featurehub.db.model.query.QDbFeatureValue
import io.featurehub.db.model.query.QDbServiceAccount
import io.featurehub.mr.model.FeatureValue
import io.featurehub.mr.model.FeatureValueType
import io.featurehub.mr.model.Person
import io.featurehub.mr.model.PersonId
import io.featurehub.mr.model.RoleType
import jakarta.inject.Inject
import jakarta.persistence.OptimisticLockException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*
import java.util.function.Function

class TestSDKFeatureUpdateServiceImpl @Inject constructor(private val updateFeatureApi: UpdateFeatureApi) :
  FeatureUpdateBySDKApi {
  private val log: Logger = LoggerFactory.getLogger(TestSDKFeatureUpdateServiceImpl::class.java)

  @Throws(RolloutStrategyValidator.InvalidStrategyCombination::class)
  override fun updateFeatureFromTestSdk(
    sdkUrl: String, envId: UUID, featureKey: String, updatingValue: Boolean,
    updatingLock: Boolean,
    buildFeatureValue: Function<FeatureValueType, FeatureValue>
  ) {
    val account = QDbServiceAccount()
      .select(QDbServiceAccount.Alias.sdkPerson.id)
      .whenArchived.isNull
      .or().apiKeyClientEval.eq(sdkUrl).apiKeyServerEval.eq(sdkUrl).endOr()
      .findOne() ?: return

    val feature = QDbApplicationFeature()
      .select(QDbApplicationFeature.Alias.id, QDbApplicationFeature.Alias.valueType)
      .parentApplication.environments.id.eq(envId).key.eq(featureKey)
      .findOne()
      ?: return

    // we need to know the current version if it exists at all

    // not checking permissions, edge checks those
    val newValue = buildFeatureValue.apply(feature.valueType)

    var fv = QDbFeatureValue().environment.id.eq(envId).feature.eq(feature).findOne()
    fv?.let {
      newValue.version(it.version)
      newValue.id(it.id)
    }

    val perms = PersonFeaturePermission(
      Person().id(PersonId().id(account.sdkPerson.id)),
      setOf(RoleType.READ, RoleType.CHANGE_VALUE, RoleType.LOCK, RoleType.UNLOCK)
    )

    var count = 0
    var saved = false
    while (count++ < 10 && !saved) {
      try {
        if (fv == null) {
          log.trace("test-sdk: creating value")
          updateFeatureApi.onlyCreateFeatureValueForEnvironment(envId, featureKey, newValue, perms)
        } else {
          updateFeatureApi.onlyUpdateFeatureValueForEnvironment(newValue, perms, fv, updatingValue, updatingLock)
        }
        saved = true
      } catch (ignored: OptimisticLockException) {
        log.trace("WARN: missed test-sdk due to optimistic lock {}", count)
        if (fv == null) {
          fv = QDbFeatureValue().environment.id.eq(envId).feature.eq(feature).findOne()
        } else {
          fv.refresh()
        }

        with(fv!!) {
          newValue.version(version)
          newValue.id(id)
        }
      }
    }
    if (!saved) {
      log.error("Unable to save feature update from TestSDK due to continual optimistic lock issues")
    }
  }
}
