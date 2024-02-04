package io.featurehub.events

import cd.connect.app.config.ConfigKey
import cd.connect.jersey.common.LoggingConfiguration
import io.cloudevents.CloudEvent
import io.featurehub.jersey.config.CommonConfiguration
import io.featurehub.lifecycle.LifecycleListener
import io.featurehub.lifecycle.LifecyclePriority
import io.featurehub.trackedevent.models.TrackedEventMethod
import io.featurehub.trackedevent.models.TrackedEventResult
import io.featurehub.utils.FallbackPropertyConfig
import jakarta.inject.Inject
import jakarta.ws.rs.client.Client
import jakarta.ws.rs.client.ClientBuilder
import jakarta.ws.rs.client.Entity
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import org.glassfish.jersey.client.ClientProperties
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*


class WebDynamicDestination(
  private val params: Map<String, String>,
  private val cloudEventType: String,
  private val destination: String,
  destSuffix: String,
  metric: CloudEventChannelMetric,
  defaultConnectTimeout: String,
  defaultReadTimeout: String,
  private val publisherRegistry: CloudEventPublisherRegistry
) {
  private val client: Client = ClientBuilder.newClient()
    .register(CommonConfiguration::class.java)
    .register(LoggingConfiguration::class.java)
  private val paramsCopy = mutableMapOf<String, String>()
  private val compressed: Boolean

  init {
    client.property(ClientProperties.CONNECT_TIMEOUT, timeout("timeout.connect", defaultConnectTimeout))
    client.property(ClientProperties.READ_TIMEOUT, timeout("timeout.read", defaultReadTimeout))

    paramsCopy.putAll(params)
    compressed = (paramsCopy.remove("compress") ?: "false") == "true"

    // support individual override of proxies
    checkParams(ClientProperties.PROXY_URI, ClientProperties.PROXY_PASSWORD, ClientProperties.PROXY_USERNAME)
  }

  fun checkParams(vararg properties: String) {
    for (property in properties) {
      if (params.containsKey(property)) {
        client.property(property, params[property])
        paramsCopy.remove(property)
      } else {
        val defaultParam = FallbackPropertyConfig.getConfig("webhooks.default.${property}")
        if (defaultParam != null) {
          client.property(property, defaultParam)
        }
      }
    }

  }

  fun publish(ce: CloudEvent) {
    val target = client.target(destination).request()

    val outboundHeaders = mutableMapOf<String, String>()

    paramsCopy.forEach { header ->
      val key = header.key

      val value = header.value
      outboundHeaders[key] = value
      target.header(key, value)
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

    try {
      log.trace("publishing CE {}:{} to {}", ce.type, ce.id, destination)

      val response = target.post(
        Entity.entity(
          ce.data?.toBytes(),
          if (compressed) "application/json+gzip" else MediaType.APPLICATION_JSON
        )
      )

      if (te != null && response != null) {
        fireCompletedTrackedEvent(te, response)
      }
    } catch (e: Exception) {
      log.error("failed to post CE {}", ce.type, e)

      if (te != null) {
        te.status(503)
        te.content(e.message?.take(1000))
      }
    }

    if (te != null) {
      publisherRegistry.publish(te)
    }
  }

  private fun fireCompletedTrackedEvent(te: TrackedEventResult, response: Response) {
    te.status(response.status)

    te.content(response.readEntity(String::class.java)?.take(1000))

    val headers = mutableMapOf<String, String?>()
    response.stringHeaders.forEach { k, v ->
      headers.put(k, v?.joinToString(";"))
    }
    te.incomingHeaders(headers)
  }

  private fun fireFailedTrackedEventOrgId(te: TrackedEventResult, e: Exception) {
    TODO("Not yet implemented")
  }

  fun timeout(key: String, defaultVal: String): Int {
    return if (params.containsKey(key)) params[key]!!.toInt() else defaultVal.toInt()
  }

  companion object {
    private val log: Logger = LoggerFactory.getLogger(WebDynamicDestination::class.java)
  }
}

@LifecyclePriority(priority = 5)
class WebDynamicPublisher @Inject constructor(
  dynamicPublisher: CloudEventDynamicPublisherRegistry,
  private val publisherRegistry: CloudEventPublisherRegistry
) : LifecycleListener {
  @ConfigKey("webhooks.features.timeout.connect")
  var connectTimeout = FallbackPropertyConfig.getConfig("webhooks.default.timeout.connect", "4000")

  @ConfigKey("webhooks.features.timeout.read")
  var readTimeout = FallbackPropertyConfig.getConfig("webhooks.default.timeout.read", "4000")

  private val log: Logger = LoggerFactory.getLogger(WebDynamicPublisher::class.java)

  init {
    dynamicPublisher.registerDymamicPublisherProvider(listOf("http://", "https://"), this::registerType)
  }

  private fun registerType(
    params: Map<String, String>,
    cloudEventType: String,
    destination: String,
    destSuffix: String,
    metric: CloudEventChannelMetric
  ) {
    log.info("registering destination webhook for cloud event {} outputting to {}", cloudEventType, destination)

    val dest = WebDynamicDestination(
      params,
      cloudEventType,
      destination,
      destSuffix,
      metric,
      connectTimeout,
      readTimeout,
      publisherRegistry
    )
    publisherRegistry.registerForPublishing(
      cloudEventType,
      metric,
      params["compress"]?.lowercase() == "false",
      dest::publish
    )
  }
}
