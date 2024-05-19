package io.featurehub.mr.resources

import io.featurehub.db.api.EnvironmentApi
import io.featurehub.db.api.FeatureHistoryApi
import io.featurehub.mr.api.FeatureHistoryServiceDelegate
import io.featurehub.mr.auth.AuthManagerService
import io.featurehub.mr.model.FeatureHistoryList
import io.featurehub.mr.model.FeatureHistoryOrder
import jakarta.inject.Inject
import jakarta.ws.rs.NotFoundException
import jakarta.ws.rs.core.SecurityContext
import java.util.*

class FeatureHistoryResource @Inject constructor(private val authManager: AuthManagerService,
                                                 private val featureHistoryApi: FeatureHistoryApi,
                                                 private val environmentApi: EnvironmentApi) : FeatureHistoryServiceDelegate {
  override fun listFeatureHistory(
    appId: UUID,
    holder: FeatureHistoryServiceDelegate.ListFeatureHistoryHolder,
    ctx: SecurityContext
  ): FeatureHistoryList {
    val person = authManager.from(ctx)
    val environmentUserHasAccessTo = environmentApi.getEnvironmentsUserCanAccess(appId, person.id!!.id) ?: throw NotFoundException()

    var envs = environmentUserHasAccessTo
    // if they specified environment ids and
    if (holder.environmentIds?.isNotEmpty() == true) {
      if (environmentUserHasAccessTo.isNotEmpty()) {
        // limited environments they can see
        envs = holder.environmentIds.filter { environmentUserHasAccessTo.contains(it) }
      } else { // else they can see any environments as they are a superuser
        envs = holder.environmentIds
      }
    }

    return featureHistoryApi.listHistory(appId, envs, holder.versions ?: listOf(),
      holder.featureKeys ?: listOf(), holder.featureIds ?: listOf(),
      holder.max, holder.startAt,  holder.order == FeatureHistoryOrder.DESC)
  }
}
