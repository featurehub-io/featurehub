package io.featurehub.edge.db.sql

import io.featurehub.encryption.WebhookEncryptionService

/**
 * This interface is required by the Conversion API but only when running as Edge Rest
 */
class EdgeRestWebhookEncryptionService : WebhookEncryptionService {
  override fun shouldEncrypt(source: Map<String, String>): Boolean {
    return false
  }

  override fun encrypt(source: Map<String, String>): Map<String, String> {
    return source
  }

  override fun encryptSingle(source: String?): String? {
    return source
  }

  override fun decryptSingle(source: String?): String? {
    return source
  }

  override fun filterEncryptedSource(source: Map<String, String>): Map<String, String> {
    return source
  }

  override fun filterAllEncryptionContent(source: Map<String, String>): Map<String, String> {
    return source
  }

  override fun filterAndReplaceWithPlaceholder(source: Map<String, String>): MutableMap<String, String> {
    return source.toMutableMap()
  }

  override fun getAllKeysEnabledForEncryption(source: Map<String, String>): List<String> {
    return emptyList()
  }

  override fun decrypt(source: Map<String, String>): Map<String, String> {
    return source
  }

  override fun decryptAndStripEncrypted(source: Map<String, String>): Map<String, String> {
    return source
  }
}
