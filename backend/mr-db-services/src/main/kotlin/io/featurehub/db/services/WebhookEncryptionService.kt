package io.featurehub.db.services

import io.featurehub.db.exception.MissingEncryptionPasswordException
import io.featurehub.encryption.SymmetricEncrypter
import io.featurehub.utils.FallbackPropertyConfig
import jakarta.inject.Inject
import jakarta.ws.rs.NotFoundException
import java.util.*

interface WebhookEncryptionService {
  fun shouldEncrypt(source: Map<String,String>): Boolean
  fun encrypt(source: Map<String, String>): Map<String,String>

  fun getAllKeysEnabledForEncryption(source: Map<String, String>): List<String>
  fun decrypt(source: Map<String, String>): Map<String,String>
}
 class WebhookEncryptionServiceImpl @Inject constructor(
  private val symmetricEncrypter: SymmetricEncrypter,
): WebhookEncryptionService {


  override fun shouldEncrypt(source: Map<String, String>): Boolean {
    return source.filter { hasEncryptSuffixAndNotEmpty(it.key, it.value) }.isNotEmpty()
  }

   fun hasEncryptSuffixAndNotEmpty(key: String, value: String): Boolean {
    return key.endsWith(".encrypt") && value.isNotEmpty()
  }

   /**
    * Returns all the map keys that are in the encrypt item
    *
    * @param source Map
    * @return List of keys in the maps whose values should be encrypted
    */

   override fun getAllKeysEnabledForEncryption(source: Map<String, String>): List<String> {
     return source.filter {
       hasEncryptSuffixAndNotEmpty(it.key, it.value)
     }.values.flatMap { it.trim().split(',') }.toList()
   }

  /**
   * Encrypts any map items which has its key in prefix.encrypt list
   *
   * @param source
   * @return Map with item values encrypted, if they need to be.
   * @throws RuntimeException if an encryption password is not set in the config item "webhooks.encryption.password"
   */
  override fun encrypt(source: Map<String, String>): Map<String, String> {
    return handleSymmetricEncryption(source) { key, value, password, result ->
      encryptItem(key, value, password, result)
    }
  }

   private fun handleSymmetricEncryption(
     source: Map<String, String>,
     callbackHandler: (key: String, value: String, password: String, result: MutableMap<String,String>) -> Unit): Map<String, String> {

     val password = FallbackPropertyConfig.getConfig("webhooks.encryption.password")
       ?: throw MissingEncryptionPasswordException()

     // copy everything from source to result
     val result = source.toMutableMap()

     val encryptItemKeys = getAllKeysEnabledForEncryption(source)

     // for each key in encrypt item keys, encrypt item and replace value, salt and encrypted
     encryptItemKeys.forEach { encryptItemKey ->
       val value = result[encryptItemKey]!!
       callbackHandler(encryptItemKey, value, password, result)
     }

     return result.toMap()
   }

   private fun encryptItem(
     key: String,
     value: String,
     password: String,
     result: MutableMap<String, String>
   ) {
     if (value == ENCRYPTEDTEXT) {
       // its already encrypted so just leave
       result[key] = value
       return
     }
     val salt = UUID.randomUUID().toString()
     val encryptedText = symmetricEncrypter.encrypt(value, password, salt)
     result["${key}.encrypted"] = encryptedText
     result[key] = ENCRYPTEDTEXT
     // we need the salt to decrypt the value so let's store it in the map
     result["${key}.salt"] = salt
   }

   /**
   * Decrypts the encrypted items in the source map
   *
   * @param key
   * @param source
   * @return
   */
  // TODO unit test this
  override fun decrypt(source: Map<String, String>): Map<String, String> {
     return handleSymmetricEncryption(source) { key, value, password, result ->
       decryptItem(key, password, source, result)
     }
  }

   private fun decryptItem(key: String, password: String, source: Map<String, String> ,result: MutableMap<String, String>) {
     val salt = source["$key.salt"] ?: throw NotFoundException("Key $key is not encrypted, salt not found!")
     val encryptedContent = source["$key.encrypted"] ?: throw NotFoundException("Key $key is not encrypted!")
     val decryptedText = symmetricEncrypter.decrypt(encryptedContent, password, salt)
     result[key] = decryptedText
   }

   companion object {
     const val ENCRYPTEDTEXT = "ENCRYPTED-TEXT"
   }

}
