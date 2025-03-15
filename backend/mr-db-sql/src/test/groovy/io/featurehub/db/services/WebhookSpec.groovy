package io.featurehub.db.services

import cd.connect.app.config.ThreadLocalConfigurationSource
import io.featurehub.db.api.FillOpts
import io.featurehub.db.api.Opts
import io.featurehub.mr.model.CreateEnvironment
import io.featurehub.mr.model.Environment
import io.featurehub.webhook.events.WebhookEnvironmentResult
import io.featurehub.webhook.events.WebhookMethod

import java.time.OffsetDateTime
import java.time.temporal.ChronoField

class WebhookSpec extends Base3Spec {
  WebhookSqlApi webhookApi

  def setup() {
    ThreadLocalConfigurationSource.createContext(['webhook.features.max-fails': '5'])
    webhookApi = new WebhookSqlApi()
  }

  def cleanup() {
    ThreadLocalConfigurationSource.clearContext()
  }

  def "i look for a non-existent detail"() {
    when:
      def result = webhookApi.getWebhookDetails(UUID.randomUUID(), UUID.randomUUID(), Opts.empty())
    then:
      !result
  }

  WebhookEnvironmentResult makeData(UUID envId, String eventType, int status = 0) {
    return new WebhookEnvironmentResult()
    .organisationId(UUID.randomUUID())
      .environmentId(envId).cloudEventType(eventType)
      .content("blah").incomingHeaders(["one": "two"]).outboundHeaders(["three": "four"])
      .result("ok").status(status).sourceSystem("mine")
      .method(WebhookMethod.POST).url("http://blah.com")
      .whenSent(OffsetDateTime.now())
  }

  def "when i record a new webhook, i can find it again"() {
    given:
      def data = makeData(env1.id, "saus", 200)
    when:
      webhookApi.saveWebhook(data)
    and:
      def result = webhookApi.paginateWebhooks(data.environmentId, 10, 0, null)
    and:
      def details = webhookApi.getWebhookDetails(env1.id, result.results[0].id, Opts.empty())
    and:
      def more = webhookApi.getWebhookDetails(env1.id, result.results[0].id, Opts.opts(FillOpts.Details))
    then:
      result.max == 1
      result.results[0].whenSent.with(ChronoField.MICRO_OF_SECOND, 0) == data.whenSent.with(ChronoField.MICRO_OF_SECOND, 0)
      result.results[0].method == 'POST'
      result.results[0].status == 200
      result.results[0].id != null
      result.results[0].type == WebhookEnvironmentResult.CLOUD_EVENT_TYPE
      details.status == 200
      details.method == 'POST'
      details.url == null
      details.sourceSystem == 'mine'
      details.result == 'ok'
      details.outboundHeaders == null
      details.incomingHeaders ==  null
      details.cloudEventType == WebhookEnvironmentResult.CLOUD_EVENT_TYPE
      details.deliveredDataCloudEventType == 'saus'
      details.content == 'blah'
      more.url == 'http://blah.com'
      more.incomingHeaders == ["one": "two"]
      more.outboundHeaders == ["three": "four"]
  }

  def "pagination works and failure deactivation works as expected"() {
    given: "i create a new environment"
      def env2 = environmentSqlApi.create(new CreateEnvironment()
        .name("pagy")
        .environmentInfo(["webhook.features.enabled": "true"])
        .description("pagy"), app1.id, superPerson)
      env2 = environmentSqlApi.get(env2.id, Opts.opts(FillOpts.Details), superPerson)
    and: "i have 40 webhooks"
      for(int count = 1; count < 40; count ++) { webhookApi.saveWebhook(makeData(env2.id, 'pootle'))}
    when: 'request 20'
      def data = webhookApi.paginateWebhooks(env2.id, 20, 0, WebhookEnvironmentResult.CLOUD_EVENT_TYPE)
    and: 'the next 25'
      def data2 = webhookApi.paginateWebhooks(env2.id, 25, 20, null)
    then:
      data.max == 39
      data.results.size() == 20
      data2.max == 39
      data2.results.size() == 19
      env2.environmentInfo['webhook.features.enabled'] == 'true'
      environmentSqlApi.get(env2.id, Opts.opts(FillOpts.Details), superPerson).environmentInfo['webhook.features.enabled'] == 'false'
  }
}
