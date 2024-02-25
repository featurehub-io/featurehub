package io.featurehub.mr.resources

import io.featurehub.db.api.SystemConfigApi
import io.featurehub.encryption.WebhookEncryptionFeature
import io.featurehub.mr.api.SystemConfigServiceDelegate
import io.featurehub.mr.auth.AuthManagerService
import io.featurehub.mr.model.SystemConfigDecryptResult
import io.featurehub.mr.model.SystemConfigs
import io.featurehub.mr.model.UpdatedSystemConfigs
import jakarta.inject.Inject
import jakarta.ws.rs.BadRequestException
import jakarta.ws.rs.ForbiddenException
import jakarta.ws.rs.NotFoundException
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
    if (!WebhookEncryptionFeature.isWebhookEncryptionEnabled) {
      throw NotFoundException()
    }

    try {
      return SystemConfigs().configs(systemConfigApi.updateConfigs(updatedSystemConfigs.configs, from.id!!.id))
    } catch (e: SystemConfigApi.UpdateSystemConfigFailedException) {
      throw BadRequestException(e.message)
    }

  }

  override fun decryptSystemConfig(
    key: String,
    holder: SystemConfigServiceDelegate.DecryptSystemConfigHolder,
    securityContext: SecurityContext
  ): SystemConfigDecryptResult {
    val from = authManagerService.from(securityContext)
    if (!authManagerService.isOrgAdmin(from)) {
      throw ForbiddenException()
    }

    if (!WebhookEncryptionFeature.isWebhookDecryptionEnabled) {
      throw NotFoundException()
    }

    try {
      val decrypt = systemConfigApi.decryptSystemConfig(key, holder.mapKey)

      return SystemConfigDecryptResult().result(decrypt)
    } catch (e: SystemConfigApi.NoSuchKeyException) {
      throw NotFoundException()
    }
  }

  override fun getSystemConfig(
    holder: SystemConfigServiceDelegate.GetSystemConfigHolder,
    securityContext: SecurityContext
  ): SystemConfigs {
    if (!WebhookEncryptionFeature.isWebhookEncryptionEnabled) {
      throw NotFoundException()
    }

    if (!authManagerService.isOrgAdmin(authManagerService.from(securityContext))) {
      throw ForbiddenException()
    }

    return SystemConfigs().configs(systemConfigApi.findConfigs(holder.filters));
  }
}
