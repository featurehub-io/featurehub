package io.featurehub.mr.resources

import io.featurehub.db.api.SystemConfigApi
import io.featurehub.mr.api.SystemConfigServiceDelegate
import io.featurehub.mr.auth.AuthManagerService
import io.featurehub.mr.model.SystemConfigs
import io.featurehub.mr.model.UpdatedSystemConfigs
import jakarta.inject.Inject
import jakarta.ws.rs.ForbiddenException
import jakarta.ws.rs.core.SecurityContext

class SystemConfigResource @Inject constructor(
  private val systemConfigApi: SystemConfigApi,
  private val authManagerService: AuthManagerService,
) : SystemConfigServiceDelegate {
  override fun createOrUpdateSystemConfigs(
    updatedSystemConfigs: UpdatedSystemConfigs,
    securityContext: SecurityContext?
  ): SystemConfigs {
    val from = authManagerService.from(securityContext)
    if (!authManagerService.isOrgAdmin(from)) {
      throw ForbiddenException()
    }

    return SystemConfigs().configs(systemConfigApi.updateConfigs(updatedSystemConfigs.configs, from.id!!.id))
  }

  override fun getSystemConfig(
    holder: SystemConfigServiceDelegate.GetSystemConfigHolder,
    securityContext: SecurityContext
  ): SystemConfigs {
    if (!authManagerService.isOrgAdmin(authManagerService.from(securityContext))) {
      throw ForbiddenException()
    }

    return SystemConfigs().configs(systemConfigApi.findConfigs(holder.filters));
  }
}
