package io.featurehub.encryption

import spock.lang.Specification

class SymmetricEncrypterIntegrationSpec extends Specification {

  def password = "SuperStrongPassword"
  def symmetricEncrypter = new SymmetricEncrypterImpl(password)

  def "should encrypt and decrypt plain text"() {
    given: "i have the source, password and salt"
      def source = "text to encrypt"

      def salt = "pink salt"
    when:
      def actual = symmetricEncrypter.encrypt(source, salt)
    then:
      actual != null
      def decryptedValue = symmetricEncrypter.decrypt(actual, salt)
      decryptedValue == source
  }

  def "should not decrypt when password is different"() {
    given: "i have the source, password and salt"
    def source = "text to encrypt"
    def password = "SuperStrongPassword"
    def salt = "pink salt"
    and:
    def encrypted = symmetricEncrypter.encrypt(source, salt)
    encrypted != null
    when:
      new SymmetricEncrypterImpl("wrongpassword").decrypt(encrypted, salt)
    then:
    thrown(RuntimeException)
  }

  def "should not decrypt when salt is different"() {
    given: "i have the source, password and salt"
    def source = "text to encrypt"
    def password = "SuperStrongPassword"
    def salt = "pink salt"
    and:
    def encrypted = symmetricEncrypter.encrypt(source, salt)
    encrypted != null
    when:
    symmetricEncrypter.decrypt(encrypted, "unknown")
    then:
    thrown(RuntimeException)
  }

}
