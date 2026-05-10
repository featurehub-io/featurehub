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
    val info = MaintenanceConfig.computeMaintenanceInfo(configs)
      ?: throw WebApplicationException(Response.noContent().build())

    return info
  }
}
