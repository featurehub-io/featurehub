package io.featurehub.mr.resources

import io.featurehub.db.api.SystemConfigApi
import io.featurehub.mr.api.MaintenanceBannerServiceDelegate
import io.featurehub.mr.model.MaintenanceInfo
import io.featurehub.systemcfg.MaintenanceConfig
import jakarta.inject.Inject
import jakarta.ws.rs.WebApplicationException
import jakarta.ws.rs.core.Response

class MaintenanceBannerResource @Inject constructor(
  private val systemConfigApi: SystemConfigApi,
) : MaintenanceBannerServiceDelegate {

  override fun getMaintenanceBanner(): MaintenanceInfo {
    if (!SystemConfigApi.systemConfigEnabled) {
      throw WebApplicationException(Response.noContent().build())
    }

    val configs = systemConfigApi.findConfigs(listOf("maintenance"))
    val active = configs.find { it.key == MaintenanceConfig.cfg_active }?.value as? Boolean ?: false

    if (!active) {
      throw WebApplicationException(Response.noContent().build())
    }

    val message = configs.find { it.key == MaintenanceConfig.cfg_message }?.value as? String
    return MaintenanceInfo().active(true).message(message)
  }
}
