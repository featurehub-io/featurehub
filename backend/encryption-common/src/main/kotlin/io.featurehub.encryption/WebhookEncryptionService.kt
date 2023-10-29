package io.featurehub.encryption

import io.featurehub.utils.FallbackPropertyConfig
import jakarta.inject.Inject
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*

interface WebhookEncryptionService {
  fun shouldEncrypt(source: Map<String, String>): Boolean
  fun encrypt(source: Map<String, String>): Map<String, String>

  /**
   * This removes any fields associated with encryption by reading the
   * ".encrypt" fields and stripping the salt/encrypted fields out. We don't
   * want to send this data back to the client as it gives them information as to what the
   * secret key likely is.
   */
  fun filterEncryptedSource(source: Map<String, String>): Map<String, String>

  fun getAllKeysEnabledForEncryption(source: Map<String, String>): List<String>
  fun decrypt(source: Map<String, String>): Map<String, String>
}

class WebhookEncryptionServiceImpl @Inject constructor(
  private val symmetricEncrypter: SymmetricEncrypter,
) : WebhookEncryptionService {
  private val log: Logger = LoggerFactory.getLogger(WebhookEncryptionServiceImpl::class.java)
  private val encryptionPassword: String;

  init {
    encryptionPassword = FallbackPropertyConfig.getMandatoryConfig("webhooks.encryption.password")
  }

  override fun shouldEncrypt(source: Map<String, String>): Boolean {
    return source.filter { hasEncryptSuffixAndNotEmpty(it.key, it.value) }.isNotEmpty()
  }

  // a field ending with ".encrypt" is a list of fields that are encrypted
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

  override fun filterEncryptedSource(source: Map<String, String>): Map<String, String> {
    val replace = source.toMutableMap()

    getAllKeysEnabledForEncryption(source).forEach { prefix ->
      replace.remove("$prefix.salt")
      replace.remove("$prefix.encryption")
    }

    return replace
  }

  private fun handleSymmetricEncryption(
    source: Map<String, String>,
    callbackHandler: (key: String, value: String, password: String, result: MutableMap<String, String>) -> Unit
  ): Map<String, String> {
    // copy everything from source to result
    val result = source.toMutableMap()

    val encryptItemKeys = getAllKeysEnabledForEncryption(source)

    // for each key in encrypt item keys, encrypt item and replace value, salt and encrypted
    encryptItemKeys.forEach { encryptItemKey ->
      val value = result[encryptItemKey]!!
      callbackHandler(encryptItemKey, value, encryptionPassword, result)
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
   * Decrypts the encrypted items in the source map.
   *
   * It finds the fields ending with ".encrypt", and then iterates through them
   * expecting to find a ".salt" and ".encrypted" name, decrypts it and stuffs it
   * into the key value.
   *
   * @param key
   * @param source
   * @return
   */
  override fun decrypt(source: Map<String, String>): Map<String, String> {
    return handleSymmetricEncryption(source) { key, value, password, result ->
      decryptItem(key, password, source, result)
    }
  }

  // we ignore any errors in the data, if someone has stuffed bad data in, we
  // will focus on ensuring the data is correct
  private fun decryptItem(
    key: String,
    password: String,
    source: Map<String, String>,
    result: MutableMap<String, String>
  ) {
    val salt = source["$key.salt"] ?: return
    val encryptedContent = source["$key.encrypted"] ?: return
    val decryptedText = symmetricEncrypter.decrypt(encryptedContent, password, salt)
    result[key] = decryptedText
  }

  companion object {
    const val ENCRYPTEDTEXT = "ENCRYPTED-TEXT"
  }

}
