package io.featurehub.encryption

import com.fasterxml.jackson.core.json.UTF8DataInputJsonParser
import com.google.common.base.Utf8
import javassist.bytecode.ByteArray
import spock.lang.Specification

import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

class SymmetricEncrypterImplTest extends Specification {

  def symmetricEncrypter = new SymmetricEncrypterImpl()

  def generateKey(password, salt) {
    def factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
    def spec = new PBEKeySpec(password.toCharArray(), salt.getBytes(), 65536, 256)
    return new SecretKeySpec(factory.generateSecret(spec).encoded, "AES")
  }

  def "should encrypt plain text"() {

    given: "i have the source, password and salt"
      def source = "text to encrypt"
      def password = "SuperStrongPassword"
      def salt = "pink salt"
      def secretKey = generateKey(password, salt)

    when:
    symmetricEncrypter.encrypt(source, password, salt)
  }

  def "Decrypt"() {
  }
}
