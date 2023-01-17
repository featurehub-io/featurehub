package io.features.webhooks.features

import cd.connect.app.config.ConfigKey
import cd.connect.app.config.DeclaredConfigResolver
import cd.connect.jersey.common.LoggingConfiguration
import io.cloudevents.CloudEvent
import io.cloudevents.core.v1.CloudEventBuilder
import io.featurehub.enriched.model.EnrichedFeatures
import io.featurehub.enricher.EnricherListenerFeature
import io.featurehub.events.CloudEventChannelMetric
import io.featurehub.events.CloudEventPublisher
import io.featurehub.events.CloudEventReceiverRegistry
import io.featurehub.jersey.config.CacheJsonMapper
import io.featurehub.jersey.config.CommonConfiguration
import io.featurehub.metrics.MetricsCollector
import io.featurehub.utils.FallbackPropertyConfig
import io.featurehub.webhook.events.WebhookEnvironmentResult
import io.featurehub.webhook.events.WebhookMethod
import jakarta.inject.Inject
import jakarta.ws.rs.client.Client
import jakarta.ws.rs.client.ClientBuilder
import jakarta.ws.rs.client.Entity
import jakarta.ws.rs.core.Feature
import jakarta.ws.rs.core.FeatureContext
import jakarta.ws.rs.core.MediaType
import org.glassfish.hk2.api.Immediate
import org.glassfish.jersey.client.ClientProperties
import org.glassfish.jersey.internal.inject.AbstractBinder
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.URI
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.time.OffsetDateTime

class WebhookFeature : Feature {
  private val log: Logger = LoggerFactory.getLogger(WebhookFeature::class.java)

  init {
    DeclaredConfigResolver.resolve(this)
  }

  override fun configure(context: FeatureContext): Boolean {
    if (enabled) {
      log.info("webhooks: registering for outbound feature webhook processing")
      context.register(EnricherListenerFeature::class.java)
      context.register(object: AbstractBinder() {
        override fun configure() {
          bind(WebhookEnricherListener::class.java).to(WebhookEnricherListener::class.java).`in`(Immediate::class.java)
        }
      })
    }

    return true
  }

  companion object {
    val enabled: Boolean

    init {
      enabled = FallbackPropertyConfig.getConfig("webhooks.features.enabled")?.lowercase() != "false"
    }
  }
}

class WebhookEnricherListener @Inject constructor(
  cloudEventReceiverRegistry: CloudEventReceiverRegistry,
  private val cloudEventPublisher: CloudEventPublisher) {
  private val client: Client

  @ConfigKey("webhooks.features.timeout.connect")
  var connectTimeout: Int? = 4000

  @ConfigKey("webhooks.features.timeout.read")
  var readTimeout: Int? = 4000

  @ConfigKey("webhooks.features.cloudevent-source")
  var cloudEventSource: String? = "https://featurehub.io"

  private val replySource = URI.create(SOURCE_SYSTEM)

  private val log: Logger = LoggerFactory.getLogger(WebhookEnricherListener::class.java)

  init {
    DeclaredConfigResolver.resolve(this)

    cloudEventReceiverRegistry.listen(EnrichedFeatures::class.java, this::process)

    client = ClientBuilder.newClient()
      .register(CommonConfiguration::class.java)
      .register(LoggingConfiguration::class.java)

    client.property(ClientProperties.CONNECT_TIMEOUT, connectTimeout)
    client.property(ClientProperties.READ_TIMEOUT, readTimeout)
  }

  fun process(ef: EnrichedFeatures, ce: CloudEvent) {
    log.info("enriched: checking for environment info")
    val conf = ef.environment.environment.environmentInfo

    log.debug("webhook: environment info is {}", conf)

    if (conf == null || (ef.targetEnrichmentDestination != null && ef.targetEnrichmentDestination != WebhookEnvironmentResult.CLOUD_EVENT_TYPE )) {
      return
    }

    if (conf[WEBHOOK_ENABLED] == "true") {
      val endpoint = conf[WEBHOOK_ENDPOINT] ?: return
      val headers = conf[WEBHOOK_HEADERS]

      // strip out management headers
      val newConf = conf.filter { !it.key.startsWith("mgmt.") }.toMap()
      ef.environment.environment.environmentInfo = newConf

      try {
        val target = client.target(endpoint).request()

        val outboundHeaders = mutableMapOf<String, String>()

        headers?.let {
          it.split(",").forEach { header ->
            val kv = header.split("=")
            if (kv.size == 2) {
              val key = URLDecoder.decode(kv[0], StandardCharsets.UTF_8)
              val value = URLDecoder.decode(kv[1], StandardCharsets.UTF_8)
              outboundHeaders[key] = value
              target.header(key, value)
            }
          }
        }

        // no bindings for jakarta yet (in 2.4.0)
        target.header("ce-specversion", "1.0")
        target.header("ce-type", EnrichedFeatures.CLOUD_EVENT_TYPE)
        target.header("ce-source", cloudEventSource)
        target.header("ce-subject", EnrichedFeatures.CLOUD_EVENT_SUBJECT)
        val time = OffsetDateTime.now()
        target.header("ce-time", time)

        val checksum = Integer.toHexString(ef.environment.featureValues.joinToString(",") { fv ->
          if (fv.value == null) {
            fv.feature.id.toString()
          } else {
            fv.value!!.id.toString() + "-" + fv.value!!.version
          }
        }.hashCode())

        val id = "${ef.environment.environment.id}-${checksum}"
        target.header("ce-id", id)

        val notifyEvent = CloudEventBuilder().newBuilder()
          .withType(WebhookEnvironmentResult.CLOUD_EVENT_TYPE)
          .withSubject(WebhookEnvironmentResult.CLOUD_EVENT_SUBJECT)
          .withSource(replySource)
          .withId(id)
          .withTime(time)

        val data = WebhookEnvironmentResult()
          .environmentId(ef.environment.environment.id)
          .method(WebhookMethod.POST)
          .sourceSystem(SOURCE_SYSTEM)
          .cloudEventType(EnrichedFeatures.CLOUD_EVENT_TYPE)
          .whenSent(time)
          .url(endpoint)
          .outboundHeaders(outboundHeaders)
          .content(CacheJsonMapper.mapper.writeValueAsString(ef))

        try {
          val response = target.post(Entity.entity(ef, MediaType.APPLICATION_JSON))

          data.status(response.status).result(response.readEntity(String::class.java)?.take(1000))
          data.incomingHeaders(mutableMapOf())
          response.stringHeaders.forEach { (k, v) ->
            data.incomingHeaders?.put(k, v.joinToString(";"))
          }
        } catch (e: Exception) {
          data.status(0).result(e.message?.take(1000))
        }

        cloudEventPublisher.publish(WebhookEnvironmentResult.CLOUD_EVENT_TYPE, data, notifyEvent)
      } catch (e: Exception) {
        log.debug("Failed to construct webhook", e)
      }
    }
  }

  companion object {
    const val WEBHOOK_ENABLED = "webhook.features.enabled"
    const val WEBHOOK_ENDPOINT = "webhook.features.endpoint"
    const val WEBHOOK_HEADERS = "webhook.features.headers" // url-encoded comma separated

    const val SOURCE_SYSTEM = "/webhook-features"
  }
}
