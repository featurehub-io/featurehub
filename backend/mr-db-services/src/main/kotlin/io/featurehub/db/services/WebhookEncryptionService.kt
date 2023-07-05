package io.featurehub.db.services

import io.featurehub.db.exception.EncryptionException
import io.featurehub.encryption.SymmetricEncrypter
import io.featurehub.utils.FallbackPropertyConfig
import jakarta.inject.Inject
import java.util.*

interface WebhookEncryptionService {
  fun shouldEncrypt(source: Map<String,String>): Boolean
  fun encrypt(source: Map<String, String>): Map<String,String>

  fun getAllKeysEnabledForEncryption(source: Map<String, String>): List<String>
  fun decrypt(encryptedContent: String): String
}
 class WebhookEncryptionServiceImpl @Inject constructor(
  private val symmetricEncrypter: SymmetricEncrypter,
): WebhookEncryptionService {


  override fun shouldEncrypt(source: Map<String, String>): Boolean {
    return source.filter { hasEncryptSuffixAndEnabled(it.key, it.value) }.isNotEmpty()
  }

   fun hasEncryptSuffixAndEnabled(key: String, value: String): Boolean {
    return key.endsWith("encrypt") && value == "true"
  }

   /**
    * Retuns all the map keys that has a corresponding "key".encrypt item as "true"
    *
    * @param source Map
    * @return List of keys in the maps whose values should be encrypted
    */

   override fun getAllKeysEnabledForEncryption(source: Map<String, String>): List<String> {
     return source.filter {
       hasEncryptSuffixAndEnabled(it.key, it.value)
     }
       .keys.map { it.replace(".encrypt", "") }
   }

  /**
   * Encrypts any map items which has a corresponding "key".encrypt with value "true"
   *
   * @param source
   * @return Map with item values encrypted, if they need to be.
   * @throws RuntimeException if an encryption password is not set in the config item "webhooks.encryption.password"
   */
  override fun encrypt(source: Map<String, String>): Map<String, String> {
    val password = FallbackPropertyConfig.getConfig("webhooks.encryption.password")
      ?: throw EncryptionException("Encryption password required!")
    val result = mutableMapOf<String, String>()

    val encryptItemKeys = getAllKeysEnabledForEncryption(source)

    source.forEach { webhookItem ->
      val key = webhookItem.key
      val value = webhookItem.value
      // We only need to encrypt the items that have a corresponding ".encrypt" item with value true
      if (encryptItemKeys.contains(key) ) {
        val salt = UUID.randomUUID()
        val encryptedText = symmetricEncrypter.encrypt(value, password, salt.toString())
        result[key] = encryptedText
        // we need the salt to decrypt the value so let's store it in the map
        result["${key}.salt"] = salt.toString()
      } else {
        result[key] = value
      }
    }
    return result.toMap()
  }


  /**
   * Returns true if the given key has a corresponding "key".encrypt with value "true" in the map
   *
   * @param key
   * @param source
   * @return
   */

  override fun decrypt(encryptedContent: String): String {
    TODO("Not yet implemented")
  }

}
