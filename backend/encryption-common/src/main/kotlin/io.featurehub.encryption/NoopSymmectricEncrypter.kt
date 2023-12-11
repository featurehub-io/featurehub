package io.featurehub.encryption

/**
 * This does nothing, it does not attempt to encrypt at all. It is used when they don't specify a password.
 */
class NoopSymmectricEncrypter : SymmetricEncrypter {
  override fun encrypt(source: String, salt: String): String {
    return source
  }

  override fun decrypt(encryptedSource: String, salt: String): String {
    return encryptedSource
  }
}
