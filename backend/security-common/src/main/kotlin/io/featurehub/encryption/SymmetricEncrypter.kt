package io.featurehub.encryption

import java.nio.ByteBuffer
import java.security.NoSuchAlgorithmException
import java.security.SecureRandom
import java.security.spec.InvalidKeySpecException
import java.security.spec.KeySpec
import java.util.*
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

interface SymmetricEncrypter {
  /**
   * Encrypts the source text using Symmetric encryption mechanism ie using the same secret key to encrypt and decrypt
   *
   * @param source text to encrypt
   * @param password to derive the secret key from
   * @param salt random value for turning the password into a secret
   * @return base64 encoded string containing the encrypted text
   */
  fun encrypt(source: String, password: String, salt: String): String

  /**
   * Decrypts the encrypted source using secret key derived from password and salt
   *
   * @param encryptedSource encrypted string to decrypt
   * @param password to derive the secret key from
   * @param salt random value for turning the password into a secret
   * @return decrypted plain text string
   */
  fun decrypt(encryptedSource: String, password: String, salt: String): String
}

data class DecodedCipher(val initializationVector: ByteArray, val cipherText: ByteArray)

class SymmetricEncrypterImpl: SymmetricEncrypter {
  private val ivLengthInBytes = 12
  private val encryptionUtils = EncryptionUtils()

  override fun encrypt(source: String, password: String, salt: String): String {

    // generate secret key using password and salt
    val secretKey = encryptionUtils.getAESKeyFromPassword(password, salt)

    // create IV
    val iv = encryptionUtils.getInitializationVector(ivLengthInBytes)

    // encrypt source using secret key and IV
    val encryptedBytes = encryptionUtils.encrypt(source, secretKey, iv)

    // base64 encode encrypted text along with IV
    return encryptionUtils.encodeCipherTextWithIv(encryptedBytes, iv)
  }

  override fun decrypt(encryptedSource: String, password: String, salt: String): String {

    // Decode and split encrypted text and IV
    val decodedCipher = encryptionUtils.decodeCipherTextWithIv(encryptedSource, ivLengthInBytes)

    // generate secret key using password and salt
    val secretKey = encryptionUtils.getAESKeyFromPassword(password, salt)

    // decrypt encrypted text using secret key and IV
    val plainTextBytes = encryptionUtils.decrypt(decodedCipher.cipherText, secretKey, decodedCipher.initializationVector)

    // decode to String
    return plainTextBytes.toString()
  }

  private inner class EncryptionUtils {
      private val secretKeyAlgorithm = "PBKDF2WithHmacSHA256"
      private val iterationCount = 65536
      private val keyLength = 256
      private val algorithm = "AES"
      private val base64Encoder = Base64.getEncoder()
      private val base64Decoder = Base64.getDecoder()
      private val cipherTransformation = "AES/GCM/NoPadding"
      private val TAG_LENGTH_BIT = 128

      /**
       * Generates a random initialization vector with the given size
       */
      fun getInitializationVector(size: Int): ByteArray {
        val iv = ByteArray(size)
        SecureRandom().nextBytes(iv)
        return iv
      }

      /**
       * Generates an AES secret key from a given password using a password-based key derivation algorithm like PBKDF2WithHmacSHA256.
       *
       * @param password password to derive the secret key from
       * @param salt random value for turning the password into a secret
       * @return the AES key from a given password with 65,536 iterations and a key length of 256 bits
       */
      @Throws(NoSuchAlgorithmException::class, InvalidKeySpecException::class)
      fun getAESKeyFromPassword(password: String, salt: String): SecretKey {
        val factory = SecretKeyFactory.getInstance(secretKeyAlgorithm)
        val spec: KeySpec = PBEKeySpec(password.toCharArray(), salt.toByteArray(), iterationCount, keyLength)
        return SecretKeySpec(factory.generateSecret(spec).encoded, algorithm)
      }

      /**
       * Prefixes initialization vector with encrypted byte array and base64 encodes the result
       *
       * @param source encrypted bytes
       * @param initializationVector random value used to encrypt data
       * @return base64 encoded string containing the encrypted text and initialization vector
       */
      fun encodeCipherTextWithIv(cipherText: ByteArray, initializationVector: ByteArray): String {

        val cipherTextWithIv = ByteBuffer.allocate(initializationVector.size + cipherText.size)
          .put(initializationVector)
          .put(cipherText)
          .array()
        return base64Encoder.encodeToString(cipherTextWithIv)

      }

      /**
       * Decodes base64 encoded text and splits it into cipherText and initialization vector
       *
       * @param source encrypted bytes with IV
       * @param ivLengthByte length of IV in bytes
       * @return DecodedCipher containing the encrypted bytes and initialization vector
       */
      fun decodeCipherTextWithIv(encryptedSource: String, ivLengthByte: Int): DecodedCipher {
        val decodedBytes = base64Decoder.decode(encryptedSource)
        val byteBuffer = ByteBuffer.wrap(decodedBytes)

        val initializationVector = ByteArray(ivLengthByte)
        byteBuffer.get(initializationVector)

        val cipherText = ByteArray(byteBuffer.remaining())
        byteBuffer.get(cipherText)
        return DecodedCipher(initializationVector, cipherText)
      }

      /**
       * Encrypts the source text using secret key and initialization vector. Uses GCM mode to provide data authenticity and confidentiality.
       *
       * @param source text to encrypt
       * @param secretKey the AES key from a given password with 65,536 iterations and a key length of 256 bits
       * @param initializationVector random value used to encrypt data. Should be 12 bytes since GCM uses 16 bytes for IV and counter
       * @return base64 encoded string containing the encrypted text and initialization vector
       */
      fun encrypt(source: String, secretKey: SecretKey, initializationVector: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(cipherTransformation)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, GCMParameterSpec(TAG_LENGTH_BIT, initializationVector))

        return cipher.doFinal(source.toByteArray())
      }

      /**
       * Decrypts encrypted bytes using secret key and initialization vector
       *
       * @param encryptedBytes
       * @param secretKey
       * @param initializationVector
       * @return
       */
      fun decrypt(encryptedBytes: ByteArray, secretKey: SecretKey, initializationVector: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(cipherTransformation)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(TAG_LENGTH_BIT, initializationVector))

        return cipher.doFinal(encryptedBytes)
      }

    }


}
