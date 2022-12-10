package io.featurehub.db.services

import cd.connect.app.config.ConfigKey
import cd.connect.app.config.DeclaredConfigResolver
import io.featurehub.db.api.FillOpts
import io.featurehub.db.api.Opts
import io.featurehub.db.api.WebhookApi
import io.featurehub.db.model.DbWebhookResult
import io.featurehub.db.model.query.QDbEnvironment
import io.featurehub.db.model.query.QDbWebhookResult
import io.featurehub.jersey.config.CacheJsonMapper
import io.featurehub.mr.model.WebhookDetail
import io.featurehub.mr.model.WebhookSummary
import io.featurehub.mr.model.WebhookSummaryItem
import io.featurehub.webhook.events.WebhookEnvironmentResult
import java.lang.Integer.min
import java.util.*

class WebhookSqlApi : WebhookApi{
  @ConfigKey("webhook.features.max-fails")
  var webhookFailsBeforeDisable: Long? = 5

  init {
    DeclaredConfigResolver.resolve(this)
  }

  override fun saveWebhook(webhookResult: WebhookEnvironmentResult) {
    QDbEnvironment().select(QDbEnvironment.Alias.id).id.eq(webhookResult.environmentId).findOne()?.let { env ->
      DbWebhookResult(
        env,
        webhookResult.whenSent,
        webhookResult.method.toString(),
        webhookResult.status,
        webhookResult.cloudEventType,
        WebhookEnvironmentResult.CLOUD_EVENT_TYPE,
        CacheJsonMapper.mapper.writeValueAsString(webhookResult)
      ).save()

      // was it a bad result? if so, we need to check the last X number of results and if they were also bad, disable
      // the webhook from sending again.
      if (webhookResult.status < 200 || webhookResult.status >= 300) {
        if (QDbWebhookResult()
            .select(QDbWebhookResult.Alias.status)
          .webhookCloudEventType.eq(WebhookEnvironmentResult.CLOUD_EVENT_TYPE)
          .environment.id.eq(webhookResult.environmentId)
            .order().whenSent.desc()
            .setMaxRows(webhookFailsBeforeDisable!!.toInt()).findList()
            .sumOf { if (it.status < 200 || it.status >= 300) 1L else 0L } == webhookFailsBeforeDisable!!
          ) {
          // need to disable and we need the existing environment data
          QDbEnvironment().select(QDbEnvironment.Alias.environmentFeatures).id.eq(webhookResult.environmentId).findOne()?.let { updateEnv ->
            updateEnv.userEnvironmentInfo["webhook.features.enabled"] = "false";
            updateEnv.save()
          }
        }
      }
    }
  }

  override fun getWebhookDetails(envId: UUID, id: UUID, opts: Opts): WebhookDetail? {
    QDbWebhookResult().id.eq(id).findOne()?.let { webhook ->
      if (webhook.environment.id != envId) {
        return null;
      }

      val detail = WebhookDetail()
        .cloudEventType(webhook.webhookCloudEventType)
        .deliveredDataCloudEventType(webhook.cloudEventType)
        .method(webhook.method)
        .status(webhook.status)

      if (webhook.webhookCloudEventType == WebhookEnvironmentResult.CLOUD_EVENT_TYPE) {
        fillWebhookEnvironmentResult(webhook, opts, detail)
      }

      return detail
    }

    return null
  }

  private fun fillWebhookEnvironmentResult(
    webhook: DbWebhookResult,
    opts: Opts,
    detail: WebhookDetail
  ) {
    val originalHook = CacheJsonMapper.mapper.readValue(webhook.json, WebhookEnvironmentResult::class.java)

    if (opts.contains(FillOpts.Details)) {
      detail
        .incomingHeaders(originalHook.incomingHeaders)
        .outboundHeaders(originalHook.outboundHeaders)
        .url(originalHook.url)
    }

    detail.content(originalHook.content).sourceSystem(originalHook.sourceSystem).result(originalHook.result)
  }

  override fun paginateWebhooks(envId: UUID, max: Int, startPos: Int, filter: String?): WebhookSummary {
    val limit = min(max.coerceAtLeast(20), 100)
    val start = startPos.coerceAtLeast(0)

    var selector = QDbWebhookResult()
      .select(QDbWebhookResult.Alias.id)
      .environment.id.eq(envId)

    filter?.let {
      selector = selector.webhookCloudEventType.eq(it)
    }

    val summary = WebhookSummary()
      .max(selector.findCount().toLong())
      .results(
        selector.setMaxRows(limit)
          .setFirstRow(start)
          .order().whenSent.desc()
          .findList().map { hook ->
            WebhookSummaryItem()
              .id(hook.id)
              .type(hook.webhookCloudEventType)
              .whenSent(hook.whenSent)
              .status(hook.status)
              .method(hook.method)
        }
      )

    return summary
  }
}
