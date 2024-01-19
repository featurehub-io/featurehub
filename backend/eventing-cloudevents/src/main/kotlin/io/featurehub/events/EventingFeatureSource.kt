package io.featurehub.events

import io.featurehub.utils.FallbackPropertyConfig
import jakarta.inject.Inject
import jakarta.ws.rs.core.Feature
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.yaml.snakeyaml.Yaml
import java.io.BufferedReader
import java.io.InputStreamReader

interface EventingFeatureSource {
  val featureSource: Class<out Feature>?
}

internal interface InternalCloudEventProperty {
  val description: String?

  /**
   * The env var or system property which stores the config
   */
  val property: String?

  /**
   * The default value if one isn't specified
   */
  val default: String?
}

internal interface InternalCloudEventCommonConfig {
  val tags: List<String>?
  val description: String?
  val ceRegistry: String?
  /**
   * The name of the property used to look up the name of this subscription/publisher if
   * different from the top level defined one. Usual in pubsub
   */
  val config: InternalCloudEventProperty?
}

internal interface InternalCloudEventSubscriberConfig : InternalCloudEventCommonConfig {
  // allows multiple subscribers in config property, comma separated
  val multiSupport: Boolean?

  // multiple listeners for the same topic, everyone gets a copy?
  val broadcast: Boolean?

  // prefix name for the subscription if it is a broadcast channel
  val name: String?
  // property to get the name of the prefix from if name isn't specified and its broadcast
  val prefixConfig: InternalCloudEventProperty?


  val conditional: InternalCloudEventProperty?
  // if provided, filter incoming events to these
  val cloudEventsInclude: List<String>?
}

internal interface InternalCloudEventPublisherConfig : InternalCloudEventCommonConfig {
  // allows multiple publishers in config property, comma separated
  val multiSupport: Boolean?
}

data class CloudEventSubscriberConfig(
  val tags: List<String>, val description: String?, val ceRegistry: String, val configs: List<String>, val config: String?,
  val broadcast: Boolean, val name: String?, val prefix: String?, val enabled: Boolean, val cloudEventsInclude: List<String>)
data class CloudEventPublisherConfig(
  val tags: List<String>, val description: String?, val ceRegistry: String, val configs: List<String>, val config: String?,
)

internal interface InternalCloudEventConfig {
  val description: String?
  val cloudEvents: List<String>?

  /**
   * Default property used to determine what the name of the channel when used with streaming
   * like kinesis and nats. Not used by PubSub.
   */
  val channelName: InternalCloudEventProperty?

  /**
   * If this conditional isn't true, all of the config underneath should be ignored.
   */
  val conditional: InternalCloudEventProperty?
  val subscribers: Map<String, InternalCloudEventSubscriberConfig>?
  val publishers: Map<String, InternalCloudEventPublisherConfig>?
}

internal interface InternalCloudEventSource {
  val nats: Map<String, InternalCloudEventConfig>
  val kinesis: Map<String, InternalCloudEventConfig>
  val pubsub: Map<String, InternalCloudEventConfig>
}

enum class CloudEventsStreamingLayer { nats, kinesis, pubsub }

data class CloudEventConfig(val name: String, val description: String?,
                            val cloudEvents: List<String>, val channelName: String?, val enabled: Boolean,
                            val subscribers: List<CloudEventSubscriberConfig>,
                            val publishers: List<CloudEventPublisherConfig>)

/**
 * this class is implemented by each messaging layer and it is responsible for finding its own config files and understanding them
 */
open class CloudEventProviderBase {
  private val log: Logger = LoggerFactory.getLogger(CloudEventProviderBase::class.java)

  fun getConfig(type: CloudEventsStreamingLayer, callback: (List<CloudEventConfig>) -> Unit ) {
    val resources = this.javaClass.classLoader.getResources("META-INF/cloud-events/cloud-events.yaml")

    while (resources.hasMoreElements()) {
      val resource = resources.nextElement()
      log.debug("found cloud-event config at {}", resource.toExternalForm())

      val config = Yaml().load<InternalCloudEventSource>(BufferedReader(InputStreamReader(resource.openStream())))

      recode(config, type)?.let { callback(it) }
    }
  }

  private fun recode(config: InternalCloudEventSource?, type: CloudEventsStreamingLayer): List<CloudEventConfig>? {
    if (config == null) return null

    return when(type) {
      CloudEventsStreamingLayer.nats -> recodeData(config.nats)
      CloudEventsStreamingLayer.kinesis -> recodeData(config.kinesis)
      CloudEventsStreamingLayer.pubsub -> recodeData(config.pubsub)
    }
  }

  private fun recodeData(source: Map<String, InternalCloudEventConfig>): List<CloudEventConfig> {
    return source.map { channel ->
      recodeChannel(channel.key, channel.value)
    }
  }

  private fun recodeChannel(name: String, config: InternalCloudEventConfig): CloudEventConfig {
    return CloudEventConfig(name, config.description, config.cloudEvents ?: listOf(),
        resolveConfig(config.channelName),
        resolveEnabled(config.conditional), recodeSubcribers(config.subscribers), recodePublishers(config.publishers))
  }

  private fun recodePublishers(publishers: Map<String, InternalCloudEventPublisherConfig>?): List<CloudEventPublisherConfig> {
    if (publishers == null) return listOf()
    return publishers.map { recodePublisher(it.key, it.value) }
  }

  private fun recodePublisher(name: String, s: InternalCloudEventPublisherConfig): CloudEventPublisherConfig {
    return CloudEventPublisherConfig(s.tags ?: listOf(), s.description, s.ceRegistry ?: "common",
      multiSupport(s.multiSupport, s.config),
      if (s.multiSupport != true) resolveConfig(s.config) else null)
  }

  private fun recodeSubcribers(subscribers: Map<String, InternalCloudEventSubscriberConfig>?): List<CloudEventSubscriberConfig> {
    if (subscribers == null) return listOf()
    return subscribers.map { recodeSubscriber(it.key, it.value) }
  }

  private fun recodeSubscriber(name: String, s: InternalCloudEventSubscriberConfig): CloudEventSubscriberConfig {
    return CloudEventSubscriberConfig(s.tags ?: listOf(), s.description, s.ceRegistry ?: "common",
      multiSupport(s.multiSupport, s.config),
      if (s.multiSupport != true) resolveConfig(s.config) else null,
      s.broadcast == true, name, s.name ?: resolveConfig(s.prefixConfig), resolveEnabled(s.conditional),
      s.cloudEventsInclude ?: listOf())
  }

  private fun multiSupport(multiSupport: Boolean?, config: InternalCloudEventProperty?): List<String> {
    if (multiSupport != true || config?.property == null) return listOf()
    val prop = resolveConfig(config) ?: return listOf()
    return prop.split(",").map { it.trim() }.filter { it.isEmpty() }
  }


  private fun resolveEnabled(conditional: InternalCloudEventProperty?): Boolean {
    if (conditional?.property == null) return true
    val prop = FallbackPropertyConfig.getConfig(conditional.property!!, conditional.default ?: "true")
    return truthy.contains(prop.lowercase())
  }

  companion object {
    val truthy = listOf("true", "1", "t")
  }

  private fun resolveConfig(config: InternalCloudEventProperty?): String? {
    if (config?.property == null ) return null

    if (config.default == null) {
      return FallbackPropertyConfig.getMandatoryConfig(config.property!!)
    } else {
      return FallbackPropertyConfig.getConfig(config.property!!, config.default!!)
    }
  }
}
