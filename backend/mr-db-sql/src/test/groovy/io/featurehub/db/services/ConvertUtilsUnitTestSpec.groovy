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
      "webhook.messaging.url":"http://someurl.com/webhook",
      "webhook.messaging.url.encrypt": "true",
      "webhook.messaging.header.Authorization":"Token",
      "webhook.messaging.header.encrypt": "true"
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
    actual.webhookEnvironmentInfo.containsKey("webhook.messaging.url")
    actual.webhookEnvironmentInfo["webhook.messaging.url"] == "ENCRYPTED-TEXT"
    actual.webhookEnvironmentInfo.containsKey("webhook.messaging.header.Authorization")
    actual.webhookEnvironmentInfo["webhook.messaging.header.Authorization"] == "ENCRYPTED-TEXT"

  }

  def "should convert to Environment with actual url in webhookEnvironmentInfo when encrypt is not enabled"() {
    given:
    def webhookEnvInfo = [
      "webhook.messaging.url":"http://someurl.com/webhook",
      "webhook.messaging.url.encrypt": "false"
    ]
    def envName = RandomStringUtils.randomAlphabetic(5)
    def parentApplication = new DbApplication()
    DbEnvironment dbEnvironment = new DbEnvironment.Builder()
      .name(envName)
      .parentApplication(parentApplication)
      .webhookEnvironmentInfo(webhookEnvInfo)
      .build()
    webhookEncryptionService.getAllKeysEnabledForEncryption(webhookEnvInfo) >> []
    def opts = new Opts(Set.of(FillOpts.Details))
    when:
    def actual = convertUtils.toEnvironment(dbEnvironment, opts, null)
    then:
    actual != null
    actual.webhookEnvironmentInfo.containsKey("webhook.messaging.url")
    actual.webhookEnvironmentInfo["webhook.messaging.url"] == "http://someurl.com/webhook"
  }
}
