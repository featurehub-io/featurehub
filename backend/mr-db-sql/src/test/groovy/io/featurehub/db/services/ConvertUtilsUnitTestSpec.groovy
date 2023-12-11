package io.featurehub.db.services

import cd.connect.app.config.ThreadLocalConfigurationSource
import io.featurehub.db.api.FillOpts
import io.featurehub.db.api.Opts
import io.featurehub.db.model.DbApplication
import io.featurehub.db.model.DbEnvironment
import io.featurehub.encryption.SymmetricEncrypter
import io.featurehub.encryption.WebhookEncryptionServiceImpl
import org.apache.commons.lang3.RandomStringUtils
import spock.lang.Specification

class ConvertUtilsUnitTestSpec extends Specification{
  SymmetricEncrypter symmetricEncrypter
  ConvertUtils convertUtils

  def setup() {
    symmetricEncrypter = Mock()
    ThreadLocalConfigurationSource.createContext([
      'webhooks.encryption.password': 'puce'
    ])
    def encryptionService = new WebhookEncryptionServiceImpl(symmetricEncrypter)
    convertUtils = new ConvertUtils(encryptionService)
  }

  def cleanup() {
    ThreadLocalConfigurationSource.clearContext()
  }

  def "should convert to Environment without encrypted value in webhookEnvironmentInfo when encrypt option is true"() {
    given:
      def webhookEnvInfo = [
        'webhook.messaging.url':'ENCRYPTED-TEXT',
        'webhook.messaging.url.encrypted': 'ENCODED-URL-TEXT',
        'webhook.messaging.url.salt': 'salt-url',
        'webhook.messaging.header.X-Info': 'soup',
        'webhook.messaging.encrypt': 'webhook.messaging.url,webhook.messaging.header.Authorization',
        'webhook.messaging.header.Authorization':'ENCRYPTED-TEXT',
        'webhook.messaging.header.Authorization.encrypted':'ENCODED-ENCRYPTED-TEXT',
        'webhook.messaging.header.Authorization.salt':'salt-auth',
      ]
      def envName = RandomStringUtils.randomAlphabetic(5)
      def parentApplication = new DbApplication()
      parentApplication.setId(UUID.randomUUID())
      DbEnvironment dbEnvironment = new DbEnvironment.Builder()
        .name(envName)
        .parentApplication(parentApplication)
        .webhookEnvironmentInfo(webhookEnvInfo)
        .build()
      dbEnvironment.setId(UUID.randomUUID())
    when: 'we ask for the data back encrypted'
      def actual = convertUtils.toEnvironment(dbEnvironment, Opts.opts(FillOpts.Details), null)
    then: 'we get filtered fields and encrypted text'
      actual != null
      def actualWebhookInfo = [
        'webhook.messaging.url':'ENCRYPTED-TEXT',
        'webhook.messaging.header.X-Info': 'soup',
        'webhook.messaging.encrypt': 'webhook.messaging.url,webhook.messaging.header.Authorization',
        'webhook.messaging.header.Authorization':'ENCRYPTED-TEXT',
      ]
      actual.webhookEnvironmentInfo == actualWebhookInfo
    when: 'we ask for the decrypted data back'
      def decrypted = convertUtils.toEnvironment(dbEnvironment, Opts.opts(FillOpts.Details, FillOpts.DecryptWebhookDetails), null)
    then:
      1 * symmetricEncrypter.decrypt('ENCODED-URL-TEXT', 'salt-url') >> 'url'
      1 * symmetricEncrypter.decrypt('ENCODED-ENCRYPTED-TEXT', 'salt-auth') >> 'auth'
      0 * _
      decrypted.webhookEnvironmentInfo == [
        'webhook.messaging.url': 'url',
        'webhook.messaging.header.X-Info': 'soup',
        'webhook.messaging.encrypt': 'webhook.messaging.url,webhook.messaging.header.Authorization',
        'webhook.messaging.header.Authorization':'auth',
      ]
  }

}
