package io.featurehub.db.services

import io.featurehub.db.api.FillOpts
import io.featurehub.db.api.Opts
import io.featurehub.db.model.DbApplication
import io.featurehub.db.model.DbEnvironment
import org.apache.commons.lang3.RandomStringUtils
import spock.lang.Specification

class ConvertUtilsUnitTestSpec extends Specification{
  WebhookEncryptionService webhookEncryptionService
  ConvertUtils convertUtils

  def setup() {
    webhookEncryptionService = Mock(WebhookEncryptionService)
    convertUtils = new ConvertUtils(webhookEncryptionService)
  }

  def "should convert to Environment without encrypted value in webhookEnvironmentInfo when encrypt option is true"() {
    given:
    def webhookEnvInfo = [
      "webhook.messaging.url":"ENCRYPTED-TEXT",
      "webhook.messaging.url.encrypted": "ENCODED-ENCRYPTED-TEXT",
      "webhook.messaging.url.salt": "salt",
      "webhook.messaging.encrypt": ["webhook.messaging.url,webhook.messaging.header.Authorization"],
      "webhook.messaging.header.Authorization":"ENCRYPTED-TEXT",
      "webhook.messaging.header.Authorization.encrypted":"ENCODED-ENCRYPTED-TEXT",
      "webhook.messaging.header.Authorization.salt":"salt",
    ]
    def envName = RandomStringUtils.randomAlphabetic(5)
    def parentApplication = new DbApplication()
    DbEnvironment dbEnvironment = new DbEnvironment.Builder()
      .name(envName)
      .parentApplication(parentApplication)
      .webhookEnvironmentInfo(webhookEnvInfo)
      .build()
    webhookEncryptionService.getAllKeysEnabledForEncryption(webhookEnvInfo) >> ["webhook.messaging.url", "webhook.messaging.header.Authorization"]
    def opts = new Opts(Set.of(FillOpts.Details))
    when:
    def actual = convertUtils.toEnvironment(dbEnvironment, opts, null)
    then:
    actual != null
    def actualWebhookInfo = [
      "webhook.messaging.url":"ENCRYPTED-TEXT",
      "webhook.messaging.encrypt": ["webhook.messaging.url,webhook.messaging.header.Authorization"],
      "webhook.messaging.header.Authorization":"ENCRYPTED-TEXT",
    ]
    actual.webhookEnvironmentInfo == actualWebhookInfo
  }

}
