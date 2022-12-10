package io.featurehub.db.api

import io.featurehub.mr.model.WebhookDetail
import io.featurehub.mr.model.WebhookSummary
import io.featurehub.webhook.events.WebhookEnvironmentResult
import java.util.*

interface WebhookApi {
  fun saveWebhook(webhookResult: WebhookEnvironmentResult)
  fun getWebhookDetails(envId: UUID, id: UUID, opts: Opts): WebhookDetail?
  fun paginateWebhooks(envId: UUID, max: Int, startPos: Int, filter: String?): WebhookSummary


}
