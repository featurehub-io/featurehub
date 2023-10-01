package io.featurehub.db.services

import io.featurehub.db.exception.MissingEncryptionPasswordException
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
    def value = "alfie"
    when:
    def actual = webhookEncryptionService.hasEncryptSuffixAndNotEmpty(key,value)
    then:
    actual

  }

  def "hasEncryptSuffixAndEnabled should return false when key ends with encrypt and value is empty"() {
    given:
    def key = "alfie.encrypt"
    def value = ""
    when:
    def actual = webhookEncryptionService.hasEncryptSuffixAndNotEmpty(key,value)
    then:
    !actual
  }

  def "hasEncryptSuffixAndEnabled should return false when key does not end with encrypt"() {
    given:
    def key = "alfie"
    def value = "true"
    when:
    def actual = webhookEncryptionService.hasEncryptSuffixAndNotEmpty(key,value)
    then:
    !actual
  }

  def "shouldEncrypt should return true when map has key ending with encrypt and value is true"() {
    given:
    def map = ["alfie.endpoint":"cool cat", "alfie.encrypt": "alfie.endpoint"]
    when:
    def actual = webhookEncryptionService.shouldEncrypt(map)
    then:
    actual
  }

  def "shouldEncrypt should return false when map has key ending with encrypt and value is empty"() {
    given:
    def map = ["alfie.encrypt": ""]
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

  def "GetAllKeysEnabledForEncryption should return keys in encrypt item"() {
    given:
    def map = ["webhook.alfie.endpoint": "cool",
               "webhook.alfie.headers.meow": "yes",
               "webhook.alfie.encrypt": "webhook.alfie.endpoint,webhook.alfie.headers.meow",
              "webhook.crookshanks.endpoint": "furry", "webhook.crookshanks.encrypt": "webhook.crookshanks.endpoint"]
    when:
    def actual = webhookEncryptionService.getAllKeysEnabledForEncryption(map)
    then:
    actual == ["webhook.alfie.endpoint","webhook.alfie.headers.meow","webhook.crookshanks.endpoint"]
  }

  def "Encrypt should throw exception when encryption password is not set"(){
    given:
    def map = ["alfie": "cool","alfie.encrypt": "true"]
    when:
    webhookEncryptionService.encrypt(map)
    then:
    thrown(MissingEncryptionPasswordException)
  }

  def "Encrypt should encrypt value"(){
    given:
    def password = "blah"
    def encrypted = "encrypted"
    System.setProperty("webhooks.encryption.password", password)

    def mapValue = "cool"
    def map = ["webhook.alfie.endpoint": mapValue, "webhook.alfie.encrypt": "webhook.alfie.endpoint"]
    when:
    def actual = webhookEncryptionService.encrypt(map)
    then:
    symmetricEncrypter.encrypt(mapValue, password, _ as String) >> encrypted
    actual != null
    actual["webhook.alfie.endpoint"] == "ENCRYPTED-TEXT"
    actual["webhook.alfie.endpoint.encrypted"] == encrypted
    actual["webhook.alfie.endpoint.salt"].length() == 36

  }

  def "Encrypt should encrypt headers"(){
    given:
    def password = "blah"
    def encrypted = "encrypted value"
    def encryptedHeader = "encrypted header value"
    System.setProperty("webhooks.encryption.password", password)

    def headerValue = "yes"
    def urlValue = "url"
    def map = [
      "webhook.alfie.feature.endpoint": urlValue,
      "webhook.alfie.feature.headers.purr": "yes",
      "webhook.alfie.feature.encrypt": "webhook.alfie.feature.endpoint,webhook.alfie.feature.headers.purr"]
    when:
    def actual = webhookEncryptionService.encrypt(map)
    then:
    symmetricEncrypter.encrypt(urlValue, password, _ as String) >> encrypted
    symmetricEncrypter.encrypt(headerValue, password, _ as String) >> encryptedHeader
    actual != null
    actual["webhook.alfie.feature.endpoint"] == "ENCRYPTED-TEXT"
    actual["webhook.alfie.feature.endpoint.encrypted"] == encrypted

    actual["webhook.alfie.feature.headers.purr"] == "ENCRYPTED-TEXT"
    actual["webhook.alfie.feature.headers.purr.encrypted"] == encryptedHeader
    actual["webhook.alfie.feature.headers.purr.salt"].length() == 36

  }

  def "Encrypt should encrypt only the headers that are enabled for encryption"(){
    given:
    def password = "blah"
    def encrypted = "encrypted value"
    def encryptedHeader = "encrypted header value"
    System.setProperty("webhooks.encryption.password", password)

    def headerValue = "loud"
    def urlValue = "url"
    def map = [
      "alfie.feature.endpoint": urlValue,
      "alfie.feature.headers.purr": "loud",
      "alfie.feature.headers.sleep": "lots",
      "alfie.feature.encrypt": "alfie.feature.endpoint,alfie.feature.headers.purr"
    ]
    when:
    def actual = webhookEncryptionService.encrypt(map)
    then:
    symmetricEncrypter.encrypt(urlValue, password, _ as String) >> encrypted
    symmetricEncrypter.encrypt(headerValue, password, _ as String) >> encryptedHeader
    actual != null
    actual["alfie.feature.endpoint"] == "ENCRYPTED-TEXT"
    actual["alfie.feature.endpoint.encrypted"] == encrypted
    actual["alfie.feature.endpoint.salt"].length() == 36
    actual["alfie.feature.headers.purr.encrypted"] == encryptedHeader
    actual["alfie.feature.headers.purr.salt"].length() == 36
    actual["alfie.feature.headers.purr"] == "ENCRYPTED-TEXT"

  }

  def "Encrypt should encrypt for multiple prefixes"(){
    given:
    def password = "blah"
    def encrypted1 = "encrypted value"
    def encryptedHeader1 = "encrypted header value"
    def encrypted2 = "encrypted value 2"
    def encryptedHeader2 = "encrypted header value 2"
    System.setProperty("webhooks.encryption.password", password)

    def headerValue1 = "loud"
    def urlValue1 = "url"
    def headerValue2 = "lotsAndLots"
    def urlValue2 = "url2"
    def map = [
      "alfie.feature.endpoint": urlValue1,
      "alfie.feature.headers.purr": "loud",
      "alfie.feature.headers.sleep": "lots",
      "alfie.messaging.endpoint": urlValue2,
      "alfie.messaging.headers.purr": "louder",
      "alfie.messaging.headers.sleep": "lotsAndLots",
      "alfie.feature.encrypt": "alfie.feature.endpoint,alfie.feature.headers.purr",
      "alfie.messaging.encrypt": "alfie.messaging.endpoint,alfie.messaging.headers.sleep"
    ]
    when:
    def actual = webhookEncryptionService.encrypt(map)
    then:
    symmetricEncrypter.encrypt(urlValue1, password, _ as String) >> encrypted1
    symmetricEncrypter.encrypt(headerValue1, password, _ as String) >> encryptedHeader1
    symmetricEncrypter.encrypt(urlValue2, password, _ as String) >> encrypted2
    symmetricEncrypter.encrypt(headerValue2, password, _ as String) >> encryptedHeader2
    actual != null
    actual["alfie.feature.endpoint"] == "ENCRYPTED-TEXT"
    actual["alfie.feature.endpoint.encrypted"] == encrypted1
    actual["alfie.feature.endpoint.salt"].length() == 36

    actual["alfie.feature.headers.purr"] == "ENCRYPTED-TEXT"
    actual["alfie.feature.headers.purr.encrypted"] == encryptedHeader1
    actual["alfie.feature.headers.purr.salt"].length() == 36


    actual["alfie.messaging.endpoint.encrypted"] == encrypted2
    actual["alfie.messaging.endpoint"] == "ENCRYPTED-TEXT"
    actual["alfie.messaging.endpoint.salt"].length() == 36

    actual["alfie.messaging.headers.sleep"] == "ENCRYPTED-TEXT"
    actual["alfie.messaging.headers.sleep.encrypted"] == encryptedHeader2
    actual["alfie.messaging.headers.sleep.salt"].length() == 36

  }
}
