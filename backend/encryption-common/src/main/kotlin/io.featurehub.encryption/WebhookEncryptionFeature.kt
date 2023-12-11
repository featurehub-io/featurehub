package io.featurehub.encryption

import io.featurehub.utils.FallbackPropertyConfig
import jakarta.inject.Singleton
import jakarta.ws.rs.core.Feature
import jakarta.ws.rs.core.FeatureContext
import org.glassfish.jersey.internal.inject.AbstractBinder

class WebhookEncryptionFeature : Feature {
  override fun configure(context: FeatureContext): Boolean {
    context.register(object : AbstractBinder() {
      override fun configure() {
        val encryptionPassword = FallbackPropertyConfig.getConfig(webhookPasswordConfig)

        if (encryptionPassword == null) {
          bind(NoopSymmectricEncrypter::class.java).to(SymmetricEncrypter::class.java).`in`(Singleton::class.java)
        } else {
          bind(SymmetricEncrypterImpl(encryptionPassword)).to(SymmetricEncrypter::class.java).`in`(Singleton::class.java)
        }

        bind(WebhookEncryptionServiceImpl::class.java).to(WebhookEncryptionService::class.java).`in`(Singleton::class.java)
      }
    })

    return true
  }

  companion object {
    const val webhookPasswordConfig = "webhooks.encryption.password"
    const val webhookDecryptionEnabled = "webhooks.decryption.enabled"

    val isWebhookEncryptionEnabled get() =
      FallbackPropertyConfig.getConfig(webhookPasswordConfig) != null

    val isWebhookDecryptionEnabled get() =
      FallbackPropertyConfig.getConfig(webhookDecryptionEnabled, "false").lowercase() == "true"
  }
}
