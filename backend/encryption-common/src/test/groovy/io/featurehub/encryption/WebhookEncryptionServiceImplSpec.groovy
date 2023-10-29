package io.featurehub.encryption

import cd.connect.app.config.ThreadLocalConfigurationSource
import spock.lang.Specification

class WebhookEncryptionServiceImplSpec extends Specification {
  WebhookEncryptionServiceImpl webhookEncryptionService
  SymmetricEncrypter symmetricEncrypter
  String password

  void setup() {
    password = 'blah'
    symmetricEncrypter = Mock(SymmetricEncrypter)
    ThreadLocalConfigurationSource.createContext([
      'webhooks.encryption.password': password
    ])
    webhookEncryptionService = new WebhookEncryptionServiceImpl(symmetricEncrypter)
  }

  def cleanup() {
    ThreadLocalConfigurationSource.clearContext()
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

  def "Encrypt should encrypt value"(){
    given:
    def encrypted = "encrypted"

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

  def "in a normal web flow, encrypted data should never leave the API layer, even into the REST layer"() {
    given: "we have encrypted data"
      def client = [
        "webhook.ginger.encrypt": "field.1,field.2",
        'field.1.salt': 'salt1',
        'field.1.encrypted': 'field1_encrypted',
        'field.1': 'ENCRYPTED-TEXT',
        'field.2.salt': 'salt2',
        'field.2.encrypted': 'field2_encrypted',
        'field.2': 'ENCRYPTED-TEXT',
      ]
    when: "i strip it ready to send to client"
      def sendToClient = webhookEncryptionService.filterEncryptedSource(client)
    then: 'we should just have raw fields + indicating of encryption'
      sendToClient.size() == 3
      sendToClient['webhook.ginger.encrypt'] == 'field.1,field.2'
      sendToClient['field.1'] == 'ENCRYPTED-TEXT'
      sendToClient['field.2'] == 'ENCRYPTED-TEXT'
    then: 'the client sends it back as is, it should not trigger shouldEncrypt'
      !webhookEncryptionService.shouldEncrypt(client + sendToClient)
    when: 'i change the text of an encrypted field'
      sendToClient['field.1'] = 'sausage'
    then:
      webhookEncryptionService.shouldEncrypt(client + sendToClient)
    when: 'i ask for the encryption to take place, field.1 changes but field.2 does not'
      def re = webhookEncryptionService.encrypt(client + sendToClient)
    then:
      1 * symmetricEncrypter.encrypt('sausage', password, _) >> 'cooked-sausage'
      re['field.2.salt'] == client['field.2.salt']
      re['field.2.encrypted'] == client['field.2.encrypted']
      re['field.1.salt'] != client['field.1.salt']
      re['field.1.encrypted'] != client['field.1.encrypted']
      re['field.1.encrypted'] == 'cooked-sausage'
      re['field.1'] == 'ENCRYPTED-TEXT'
      re['field.2'] == 'ENCRYPTED-TEXT'
      re['webhook.ginger.encrypt'] == 'field.1,field.2'
    when: "we publish this via a webhook we should strip out any encrypted content"
      def publish = webhookEncryptionService.filterAllEncryptionContent(re)
    then:
      publish.size() == 2
      publish['field.1'] == 'ENCRYPTED-TEXT'
      publish['field.2'] == 'ENCRYPTED-TEXT'
  }

  def "Encrypt should encrypt headers"(){
    given:
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
    def encrypted = "encrypted value"
    def encryptedHeader = "encrypted header value"

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
    def encrypted1 = "encrypted value"
    def encryptedHeader1 = "encrypted header value"
    def encrypted2 = "encrypted value 2"
    def encryptedHeader2 = "encrypted header value 2"

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

  def "decrypt valid setup works as expected"() {
    given: "i have an encrypted map"
        def encrypted_map = [
          'webhook.messaging.url': 'ENCRYPTED-TEXT',
          'webhook.messaging.url.encrypted': 'blah',
          'webhook.messaging.url.salt': 'pepper',
          'webhook.messaging.encrypt': 'webhook.messaging.url'
        ]
    when: 'I decrypt the data'
      def result = webhookEncryptionService.decrypt(encrypted_map)
    then:
      1 * symmetricEncrypter.decrypt('blah', password, 'pepper') >> 'sausage'
      result.size() == 4
      result['webhook.messaging.url'] == 'sausage'
    when: 'I filter the decrypted data'
      def filteredDecrypted = webhookEncryptionService.filterEncryptedSource(result)
    then:
      filteredDecrypted.size() == 2
      filteredDecrypted['webhook.messaging.url'] == 'sausage'
      filteredDecrypted['webhook.messaging.encrypt'] == 'webhook.messaging.url'
    when: "i reencrypt"
      def reencrypt = webhookEncryptionService.encrypt(result)
    then:
      1 * symmetricEncrypter.encrypt('sausage', password, _) >> 'ENCRYPTED-TEXT'
      reencrypt.size() == 4
      reencrypt['webhook.messaging.url'] == 'ENCRYPTED-TEXT'
    when: "i filter encrypted"
      def filterEncrypted = webhookEncryptionService.filterEncryptedSource(reencrypt)
    then:
      filterEncrypted.size() == 2
      filterEncrypted['webhook.messaging.url'] == 'ENCRYPTED-TEXT'
      filterEncrypted['webhook.messaging.encrypt'] == 'webhook.messaging.url'
  }
}
