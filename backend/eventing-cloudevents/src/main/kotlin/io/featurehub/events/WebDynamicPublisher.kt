package io.featurehub.events

import cd.connect.app.config.ConfigKey
import cd.connect.jersey.common.LoggingConfiguration
import io.cloudevents.CloudEvent
import io.featurehub.encryption.WebhookEncryptionService
import io.featurehub.jersey.config.CommonConfiguration
import io.featurehub.lifecycle.LifecycleListener
import io.featurehub.lifecycle.LifecyclePriority
import io.featurehub.trackedevent.models.TrackedEventMethod
import io.featurehub.trackedevent.models.TrackedEventResult
import io.featurehub.utils.FallbackPropertyConfig
import jakarta.inject.Inject
import jakarta.ws.rs.WebApplicationException
import jakarta.ws.rs.client.Client
import jakarta.ws.rs.client.ClientBuilder
import jakarta.ws.rs.client.Entity
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import org.glassfish.jersey.client.ClientProperties
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*


open class BaseWebhook {
  protected val client: Client = ClientBuilder.newClient()
    .register(CommonConfiguration::class.java)
    .register(LoggingConfiguration::class.java)

  init {
    // support individual override of proxies
    checkParams(ClientProperties.PROXY_URI, ClientProperties.PROXY_PASSWORD, ClientProperties.PROXY_USERNAME)
  }

  private fun checkParams(vararg properties: String) {
    for (property in properties) {
      val propertyName = if (property.startsWith(WebDynamicDestination.JERSEY_PREFIX)) property.substring(
        WebDynamicDestination.JERSEY_PREFIX.length) else property
      val defaultParam = FallbackPropertyConfig.getConfig("webhooks.default.${propertyName}")
      if (defaultParam != null) {
        client.property(property, defaultParam)
      }
    }
  }

  protected fun captureException(e: Exception, te: TrackedEventResult) {
    if (e is WebApplicationException) {
      te.status(e.response.status)
      te.content(e.response.readEntity(String::class.java)?.take(1000))
    } else {
      te.status(503)
      te.content(e.message?.take(1000))
    }
  }

  protected fun captureCompletedWebPost(te: TrackedEventResult, response: Response) {
    te.status(response.status)

    te.content(response.readEntity(String::class.java)?.take(1000))

    val headers = mutableMapOf<String, String?>()
    response.stringHeaders.forEach { (k, v) ->
      headers[k] = v?.joinToString(";")
    }
    te.incomingHeaders(headers)
  }

  fun timeout(connectTimeout: String, readTimeout: String) {
    client.property(ClientProperties.CONNECT_TIMEOUT, connectTimeout)
    client.property(ClientProperties.READ_TIMEOUT, readTimeout)
  }

}

class WebDynamicDestination(
  private val publisherRegistry: CloudEventPublisherRegistry,
  private val webhookEncryptionService: WebhookEncryptionService
) : BaseWebhook() {
  fun publish(
    ce: CloudEvent,
    config: CloudEventDynamicDeliveryDetails,
    metric: CloudEventChannelMetric,
    destination: String
  ) {
    val target = client.target(destination).request()

    config.headers?.let { headers ->
      webhookEncryptionService.decrypt(headers).let { resultingHeaders ->
        resultingHeaders.forEach { header ->
          if (header.value.isNotEmpty() && !header.key.endsWith(".encrypt"))
            target.header(header.key, header.value)
        }
      }
    }

    ce.attributeNames.forEach { name ->
      target.header("ce-${name}", ce.getAttribute(name))
    }

    ce.extensionNames.forEach { name ->
      target.header("ce-$name", ce.getExtension(name))
    }

    val trackedEventOrgId = ce.getExtension("trackedevent") as String?

    val te = if (trackedEventOrgId == null) null else TrackedEventResult()
      .method(TrackedEventMethod.POST)
      .status(500)
      .originatingCloudEventMessageId(UUID.fromString(ce.id))
      .originatingOrganisationId(UUID.fromString(trackedEventOrgId))
      .originatingCloudEventType(ce.type)

    val perfTimer = metric.perf.startTimer()

    try {
      log.trace("publishing CE {}:{} to {}", ce.type, ce.id, config.url!!)

      // we match the content-type to the data-contenttype
      val response = target.post(
        Entity.entity(
          ce.data?.toBytes(),
          if (config.compressed) "application/json+gzip" else MediaType.APPLICATION_JSON
        )
      )

      if (te != null && response != null) {
        captureCompletedWebPost(te, response)
      }
    } catch (e: Exception) {
      metric.failures.inc()

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

  companion object {
    private val log: Logger = LoggerFactory.getLogger(WebDynamicDestination::class.java)

    const val JERSEY_PREFIX = "jersey.config.client."
  }
}

@LifecyclePriority(priority = 5)
class WebDynamicPublisher @Inject constructor(
  dynamicPublisher: CloudEventDynamicPublisherRegistry,
  publisherRegistry: CloudEventPublisherRegistry,
  webhookEncryptionService: WebhookEncryptionService,
) : LifecycleListener {
  @ConfigKey("webhooks.default.timeout.connect")
  var connectTimeout = FallbackPropertyConfig.getConfig("webhooks.default.timeout.connect", "4000")

  @ConfigKey("webhooks.default.timeout.read")
  var readTimeout = FallbackPropertyConfig.getConfig("webhooks.default.timeout.read", "4000")

  private val log: Logger = LoggerFactory.getLogger(WebDynamicPublisher::class.java)
  private val webhookDestination = WebDynamicDestination(publisherRegistry, webhookEncryptionService).apply {
    timeout(connectTimeout, readTimeout)
  }

  init {
    dynamicPublisher.registerDynamicPublisherProvider(listOf("http://", "https://"), this::publish)
  }

  private fun publish(
    config: CloudEventDynamicDeliveryDetails,
    cloudEvent: CloudEvent,
    destination: String,
    destSuffix: String,
    metric: CloudEventChannelMetric
  ) {
    log.info("registering destination webhook for cloud event {} outputting to {}", cloudEvent.type, destination)

    webhookDestination.publish(cloudEvent, config, metric, destination)
  }
}
