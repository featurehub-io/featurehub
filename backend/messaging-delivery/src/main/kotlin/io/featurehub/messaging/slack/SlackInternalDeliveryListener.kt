package io.featurehub.messaging.slack

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import io.cloudevents.CloudEvent
import io.featurehub.encryption.WebhookEncryptionService
import io.featurehub.events.BaseWebhook
import io.featurehub.events.CloudEventPublisherRegistry
import io.featurehub.events.CloudEventReceiverRegistry
import io.featurehub.lifecycle.LifecycleListener
import io.featurehub.lifecycle.LifecyclePriority
import io.featurehub.messaging.model.FeatureMessagingUpdate
import io.featurehub.messaging.utils.FeatureMessageFormatter
import io.featurehub.metrics.MetricsCollector
import io.featurehub.trackedevent.models.TrackedEventMethod
import io.featurehub.trackedevent.models.TrackedEventResult
import jakarta.inject.Inject
import jakarta.ws.rs.client.Entity
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*


@JsonIgnoreProperties(ignoreUnknown = true)
data class SlackDeliveryResponse(val ok: Boolean?, val error: String?)

@LifecyclePriority(priority = 10)
class SlackWebClient @Inject constructor(
  private val encryptionService: WebhookEncryptionService,
  private val featureMessageFormatter: FeatureMessageFormatter,
  private val publisherRegistry: CloudEventPublisherRegistry,
  private val receiverRegistry: CloudEventReceiverRegistry
) : BaseWebhook(), LifecycleListener {
  private val perf = MetricsCollector.histogram("slack_publish", "How many times we have published to Slack")
  private val failures = MetricsCollector.counter("slack_failures", "How many times we have failed to publish to Slack")
  private val target = client.target("https://slack.com/api/chat.postMessage")

  init {
    receiverRegistry.listen(FeatureMessagingUpdate::class.java, "integration/slack-v1", null, this::process)
  }

  companion object {
    private val log: Logger = LoggerFactory.getLogger(SlackWebClient::class.java)
    const val DEFAULT_MESSAGE_FORMAT =
      """----------------------------------------------------------------------------
Feature *{{fName}}* (`{{fKey}}`) in *{{ eName }}* was changed by *{{ whoUpdated }}* at {{ whenUpdatedReadable }}

Summary of changes:
{{#featureValueUpdated}}>• Default value now `{{{updated}}}` was `{{{previous}}}`{{/featureValueUpdated}}
{{~#if updatedStrategies}}
{{#updatedStrategies}}{{~#if nameChanged}}>• Strategy name changed from `{{oldStrategy.name}}` to `{{newStrategy.name}}`   
{{else}}{{~#if valueChanged}}>• *{{newStrategy.name}}* strategy value set to `{{{newStrategy.value}}}`
{{else}}>• *{{newStrategy.name}}* strategy rules have been updated
{{/if}}{{/if}}{{/updatedStrategies}}{{/if}}
{{~#if addedStrategies}}
>• Added new strategies: {{#addedStrategies}}*{{name}}*{{#unless @last}}, {{/unless}}{{/addedStrategies}}{{/if}}
{{~#strategiesReordered}}>• Strategies were re-ordered from {{#previous}}*{{name}}*, {{/previous}} to {{#reordered}}*{{name}}*{{^last}}{{#unless @last}}, {{/unless}} {{/last}}{{/reordered}}{{/strategiesReordered}}
{{~#if deletedStrategies}}
>• Deleted strategies: {{#deletedStrategies}}*{{name}}*{{#unless @last}}, {{/unless}}
{{/deletedStrategies}}{{/if}}
{{~#lockUpdated~}}
{{~#wasLocked}}>• Feature set to `locked`{{/wasLocked}}
{{~^wasLocked}}>• Feature set to `unlocked`{{/wasLocked}}{{~/lockUpdated~}}
{{~#retiredUpdated}}
>• {{~#if wasRetired}} Feature set to `retired`{{/if}}{{#unless wasRetired}} Feature set to `unretired`{{/unless}}{{/retiredUpdated}}

Portfolio: *{{ pName }}*, Application: *{{ aName }}*
----------------------------------------------------------------------------"""
  }

  internal data class PreprocessSlack(
    val channel: String,
    val bearer: String,
    val messageFmt: String,
    val info: Map<String, String>
  )

  private fun preprocess(fmUpdate: FeatureMessagingUpdate): PreprocessSlack? {
    if (fmUpdate.additionalInfo == null) {
      log.error("Received Slack process message with no info")
      return null
    }

    val info = fmUpdate.additionalInfo!!

    var url = info["site.url"] ?: "http://localhost:8085"
    if (url.endsWith("/")) {
      url = url.substring(0, url.length - 1)
    }
    // wipe this so the message formatter can't get it
    fmUpdate.additionalInfo = mapOf(Pair("site_url", url))
    if (info["slack.channel"] == null) {
      log.error("Received Slack message with no channel, earlier code should have prevented")
      return null
    }

    val channel = info["slack.channel"]!!

    val fmt = info["slack.messageFormat"] ?: DEFAULT_MESSAGE_FORMAT

    if (info["slack.token"] == null) {
      log.error("received slack message with no bearer token")
      return null
    }

    val token = info["slack.token"]!!

    try {
      val bearer = encryptionService.decryptSingle(token)

      if (bearer == null) {
        log.error("received slack token that was undecryptable")
        return null
      }

      return PreprocessSlack(channel, bearer, fmt, info)
    } catch (e: Exception) {
      log.error("Unable to decode bearer token", e)
      return null
    }
  }

  private fun bakeMeASlackCake(message: String, channelName: String): String {
    val insertMessage = message.replace("\"", "\\\"")
    val channel = channelName.replace("\"", "\\\"")
    return """{"channel": "$channel", "blocks":[{"type":"section", "text":{"type":"mrkdwn","text":"$insertMessage"}}]}"""
  }

  fun process(
    fmUpdate: FeatureMessagingUpdate, ce: CloudEvent
  ) {
    // extract the valid config or return
    val config = preprocess(fmUpdate) ?: return

    val te = TrackedEventResult()
      .method(TrackedEventMethod.POST)
      .status(500)
      .originatingCloudEventMessageId(UUID.fromString(ce.id))
      .originatingOrganisationId(fmUpdate.organizationId)
      .originatingCloudEventType(ce.type)

    val perfTimer = perf.startTimer()

    try {
      val msg = featureMessageFormatter.enhanceMessagingUpdateForHandlebars(fmUpdate)
      val formatted = featureMessageFormatter.formatMessage(msg, DEFAULT_MESSAGE_FORMAT)

      log.trace("publishing Slack {}:{}", ce.type, ce.id)
      val request = target.request()
        .header("Authorization", "Bearer ${config.bearer}")

      val response = request.post(
        Entity.entity(
          bakeMeASlackCake(formatted, config.channel),
          "application/json; charset=utf-8"
        )
      )

      if (te != null) {
        if (response?.status == 200) {
          val data = response.readEntity(SlackDeliveryResponse::class.java)
          if (data.ok == false) {
            te.status = 400
            te.content = "Slack message failed with code: '${data.error ?: "unknown error"}'"
          } else if (data.ok == true) {
            te.status = 200
            te.content = "Slack message posted successfully"
          } else {
            te.status = 503
            te.content = "Slack communication unknown"
          }
        } else {
          captureCompletedWebPost(te, response)
        }
      }
    } catch (e: Exception) {
      failures.inc()

      log.error("failed to post CE {}", ce.type, e)

      if (te != null) {
        captureException(e, te)
      }
    } finally {
      perfTimer.observeDuration()
    }

    if (te != null) {
      publisherRegistry.publish(te)
    }
  }
}
