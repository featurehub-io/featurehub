package io.featurehub.db.services

import io.featurehub.encryption.SymmetricEncrypter
import spock.lang.Specification

class WebhookEncryptionServiceImplSpec extends Specification {
  WebhookEncryptionServiceImpl webhookEncryptionService
  SymmetricEncrypter symmetricEncrypter

  void setup() {
    symmetricEncrypter = Mock(SymmetricEncrypter)
    webhookEncryptionService = new WebhookEncryptionServiceImpl(symmetricEncrypter)
  }

  def "hasEncryptSuffixAndEnabled should return true when key ends with encrypt and value is true"() {
    given:
    def key = "alfie.encrypt"
    def value = "true"
    when:
    def actual = webhookEncryptionService.hasEncryptSuffixAndEnabled(key,value)
    then:
    actual

  }

  def "hasEncryptSuffixAndEnabled should return false when key ends with encrypt and value is false"() {
    given:
    def key = "alfie.encrypt"
    def value = "false"
    when:
    def actual = webhookEncryptionService.hasEncryptSuffixAndEnabled(key,value)
    then:
    !actual
  }

  def "hasEncryptSuffixAndEnabled should return false when key does not end with encrypt"() {
    given:
    def key = "alfie"
    def value = "true"
    when:
    def actual = webhookEncryptionService.hasEncryptSuffixAndEnabled(key,value)
    then:
    !actual
  }

  def "shouldEncrypt should return true when map has key ending with encrypt and value is true"() {
    given:
    def map = ["alfie":"cool cat", "alfie.encrypt": "true"]
    when:
    def actual = webhookEncryptionService.shouldEncrypt(map)
    then:
    actual
  }

  def "shouldEncrypt should return false when map has key ending with encrypt and value is false"() {
    given:
    def map = ["alfie.encrypt": "false"]
    when:
    def actual = webhookEncryptionService.shouldEncrypt(map)
    then:
    !actual
  }

  def "shouldEncrypt should return false when map has no key ending with encrypt"() {
    given:
    def map = ["alfie": "cool"]
    when:
    def actual = webhookEncryptionService.shouldEncrypt(map)
    then:
    !actual
  }

  def "Encrypt"() {
  }

  def "GetAllKeysEnabledForEncryption should return key ending with encrypt and has value true"() {
    given:
    def map = ["alfie": "cool","alfie.encrypt": "true"]
    when:
    def actual = webhookEncryptionService.getAllKeysEnabledForEncryption(map)
    then:
    actual == ["alfie"]
  }

  def "Encrypt should throw exception when encryption password is not set"(){
    given:
    def map = ["alfie": "cool","alfie.encrypt": "true"]
    when:
    def actual = webhookEncryptionService.encrypt(map)
    then:
    thrown(RuntimeException)
  }

  def "Encrypt should encrypt value"(){
    given:
    def password = "blah"
    def encrypted = "encrypted"
    System.setProperty("webhooks.encryption.password", password)

    def mapValue = "cool"
    def map = ["alfie": mapValue, "alfie.encrypt": "true"]
    when:
    def actual = webhookEncryptionService.encrypt(map)
    then:
    symmetricEncrypter.encrypt(mapValue, password, _ as String) >> encrypted
    actual != null
    actual["alfie"] == encrypted
    actual["alfie.salt"].length() == 36

  }
}
