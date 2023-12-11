package io.featurehub.mr.resources

import io.cloudevents.core.v1.CloudEventBuilder
import io.featurehub.db.api.FillOpts
import io.featurehub.db.api.Opts
import io.featurehub.db.api.WebhookApi
import io.featurehub.enriched.model.EnricherPing
import io.featurehub.events.CloudEventPublisher
import io.featurehub.messaging.model.FeatureMessagingUpdate
import io.featurehub.mr.api.WebhookServiceDelegate
import io.featurehub.mr.auth.AuthManagerService
import io.featurehub.mr.model.*
import io.featurehub.webhook.events.WebhookEnvironmentResult
import jakarta.inject.Inject
import jakarta.ws.rs.ForbiddenException
import jakarta.ws.rs.NotFoundException
import jakarta.ws.rs.core.SecurityContext
import java.net.URI
import java.time.OffsetDateTime
import java.util.*

class WebhookResource @Inject constructor(
  private val webhookApi: WebhookApi,
  private val cloudEventPublisher: CloudEventPublisher,
  private val authManagerService: AuthManagerService
) : WebhookServiceDelegate {
  override fun getWebhookDetails(envId: UUID, id: UUID, securityContext: SecurityContext): WebhookDetail {
    val person = authManagerService.from(securityContext)

    if (authManagerService.isOrgAdmin(person) || authManagerService.isPortfolioAdminOfEnvironment(envId, person)) {
      return webhookApi.getWebhookDetails(envId, id, Opts.opts(FillOpts.Details)) ?: throw NotFoundException()
    }

    throw ForbiddenException()
  }

  override fun getWebhookTypes(): WebhookTypeDetails {
    return WebhookTypeDetails()
      .types(
        listOf(
          WebhookTypeDetail()
            .messageType(WebhookEnvironmentResult.CLOUD_EVENT_TYPE)
            .envPrefix("webhook.features")
            .description("Webhook: Feature updates"),
          WebhookTypeDetail()
            .messageType("integration/slack-v1")
            .envPrefix("integration.slack")
            .description("Slack")
//          WebhookTypeDetail()
//            .messageType(FeatureMessagingUpdate.CLOUD_EVENT_TYPE)
//            .envPrefix("webhook.messaging")
//            .description("Feature updates for messaging integration")
        )
      )
  }

  override fun listWebhooks(
    envId: UUID,
    holder: WebhookServiceDelegate.ListWebhooksHolder,
    securityContext: SecurityContext
  ): WebhookSummary {
    val person = authManagerService.from(securityContext)

    if (authManagerService.isOrgAdmin(person) || authManagerService.isPortfolioAdminOfEnvironment(envId, person)) {
      return webhookApi.paginateWebhooks(envId, holder.max ?: 10, holder.startAt ?: 0, holder.filter)
    }

    throw ForbiddenException()
  }

  override fun testWebhook(webhookCheck: WebhookCheck, securityContext: SecurityContext?) {
    val person = authManagerService.from(securityContext)

    if (authManagerService.isOrgAdmin(person) || authManagerService.isPortfolioAdminOfEnvironment(webhookCheck.envId, person)) {
      cloudEventPublisher.publish(
        EnricherPing.CLOUD_EVENT_TYPE,
        EnricherPing()
          .cloudEventProcessor(webhookCheck.messageType)
          .environment(webhookCheck.envId),
        CloudEventBuilder()
          .newBuilder()
          .withId(UUID.randomUUID().toString())
          .withSource(URI.create("/webhook/check"))
          .withTime(OffsetDateTime.now())
      )

      return
    }

    throw ForbiddenException()
  }
}
