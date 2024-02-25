package io.featurehub.encryption

import jakarta.inject.Inject
import java.util.*

interface WebhookEncryptionService {
  fun shouldEncrypt(source: Map<String, String>): Boolean
  fun encrypt(source: Map<String, String>): Map<String, String>

  /**
   * Single strings need to store their encrypted text and the salt in a single string. We encode this using
   * a tab character to separate them. So its <salt><tab><encrypted>
   */
  fun encryptSingle(source: String?): String?

  fun decryptSingle(source: String?): String?

  /**
   * This removes any fields associated with encryption by reading the
   * ".encrypt" fields and stripping the salt/encrypted fields out. We don't
   * want to send this data back to the client as it gives them information as to what the
   * secret key likely is.
   */
  fun filterEncryptedSource(source: Map<String, String>): Map<String, String>

  /**
   * This is used by the outgoing webhook to remove all encryption related content, including
   * .salt, .encrypted and .encrypt fields
   */
  fun filterAllEncryptionContent(source: Map<String, String>): Map<String, String>

  /**
   * This will take all encrypted fields and replace their values with ENCRYPTEDTEXT, but leave in place
   * the special key that indicates which fields are encrypted.
   *
   * This leaves all fields with the ".encrypted" suffix in-situ to indicate which fields were actually encrypted.
   */
  fun filterAndReplaceWithPlaceholder(source: Map<String, String>): MutableMap<String, String>

  fun getAllKeysEnabledForEncryption(source: Map<String, String>): List<String>
  fun decrypt(source: Map<String, String>): Map<String, String>
  fun decryptAndStripEncrypted(source: Map<String, String>): Map<String, String>

  companion object {
    const val ENCRYPTEDTEXT = "ENCRYPTED-TEXT"
  }
}

class WebhookEncryptionServiceImpl @Inject constructor(
  private val symmetricEncrypter: SymmetricEncrypter,
) : WebhookEncryptionService {

  override fun encryptSingle(source: String?): String? {
    if (source == null) return null
    if (source == WebhookEncryptionService.ENCRYPTEDTEXT) return source

    val salt = UUID.randomUUID().toString()
    val encryptedText = symmetricEncrypter.encrypt(source, salt)

    return "${salt}\t${encryptedText}"
  }

  override fun decryptSingle(source: String?): String? {
    if (source == null) return null
    if (source == WebhookEncryptionService.ENCRYPTEDTEXT) return source
    val pos = source.indexOf("\t")
    if (pos == -1) return source // not encrypted
    val salt = source.substring(0, pos)
    val encryptedText = source.substring(pos + 1)
    if (salt.isEmpty() || encryptedText.isEmpty()) return source
    return symmetricEncrypter.decrypt(encryptedText, salt)
  }

  override fun shouldEncrypt(source: Map<String, String>): Boolean {
    return getAllKeysEnabledForEncryption(source).any {
      source[it] != WebhookEncryptionService.ENCRYPTEDTEXT
    }
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
   */
  override fun encrypt(source: Map<String, String>): Map<String, String> {
    val updated = handleSymmetricEncryption(source) { key, value, result ->
      encryptItem(key, value, result)
    }

    val validSaltsAndEncrypts = getAllKeysEnabledForEncryption(updated).map { listOf("$it.salt", "$it.encrypted") }.flatten()

    val data = updated.filter {
      ((it.key.endsWith(".salt") || it.key.endsWith(".encrypted")) && validSaltsAndEncrypts.contains(it.key)) ||
        (!it.key.endsWith(".salt") && !it.key.endsWith(".encrypted"))
     }

    return data
  }

  override fun filterEncryptedSource(source: Map<String, String>): Map<String, String> {
    val replace = source.toMutableMap()

    getAllKeysEnabledForEncryption(source).forEach { prefix ->
      replace.remove("$prefix.salt")
      replace.remove("$prefix.encrypted")
    }

    return replace
  }

  override fun filterAllEncryptionContent(source: Map<String, String>): Map<String, String> {
    return filterEncryptedSource(source).filter { !it.key.endsWith(".encrypt") }
  }

  override fun filterAndReplaceWithPlaceholder(source: Map<String, String>): MutableMap<String, String> {
    val replace = source.toMutableMap()

    getAllKeysEnabledForEncryption(source).forEach { prefix ->
      replace.remove("$prefix.salt")
      replace.remove("$prefix.encrypted")
      replace[prefix] = WebhookEncryptionService.ENCRYPTEDTEXT
    }

    return replace
  }

  private fun handleSymmetricEncryption(
    source: Map<String, String>,
    callbackHandler: (key: String, value: String, result: MutableMap<String, String>) -> Unit
  ): Map<String, String> {
    // copy everything from source to result
    val result = source.toMutableMap()

    val encryptItemKeys = getAllKeysEnabledForEncryption(source)

    // for each key in encrypt item keys, encrypt item and replace value, salt and encrypted
    encryptItemKeys.forEach { encryptItemKey ->
      val value = result[encryptItemKey]!!
      callbackHandler(encryptItemKey, value, result)
    }

    return result.toMap()
  }

  private fun encryptItem(
    key: String,
    value: String,
    result: MutableMap<String, String>
  ) {
    if (value == WebhookEncryptionService.ENCRYPTEDTEXT) {
      // its already encrypted so just leave
      result[key] = value
      return
    }
    val salt = UUID.randomUUID().toString()
    val encryptedText = symmetricEncrypter.encrypt(value, salt)
    result["${key}.encrypted"] = encryptedText
    result[key] = WebhookEncryptionService.ENCRYPTEDTEXT
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
   * @return decrypted data
   */
  override fun decrypt(source: Map<String, String>): Map<String, String> {
    return handleSymmetricEncryption(source) { key, value, result ->
      decryptItem(key, source, result)
    }
  }

  override fun decryptAndStripEncrypted(source: Map<String, String>): Map<String, String> {
    return handleSymmetricEncryption(source) { key, value, result ->
      decryptItem(key, source, result)
      result.remove("$key.salt")
      result.remove("$key.encrypted")
    }

  }

  // we ignore any errors in the data, if someone has stuffed bad data in, we
  // will focus on ensuring the data is correct
  private fun decryptItem(
    key: String,
    source: Map<String, String>,
    result: MutableMap<String, String>
  ) {
    val salt = source["$key.salt"] ?: return
    val encryptedContent = source["$key.encrypted"] ?: return
    val decryptedText = symmetricEncrypter.decrypt(encryptedContent, salt)
    result[key] = decryptedText
  }

}
