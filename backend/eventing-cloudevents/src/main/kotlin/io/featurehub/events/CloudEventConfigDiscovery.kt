package io.featurehub.events

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import io.cloudevents.CloudEvent
import io.featurehub.metrics.MetricsCollector
import io.featurehub.rest.Info
import io.featurehub.utils.FallbackPropertyConfig
import jakarta.inject.Inject
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.yaml.snakeyaml.Yaml
import java.io.BufferedReader
import java.io.InputStreamReader

internal data class InternalCloudEventProperty(
  val description: String?,

  /**
   * The env var or system property which stores the config
   */
  val property: String?,

  /**
   * The default value if one isn't specified
   */
  val default: String?
)

internal data class InternalCloudEventSubscriberConfig(
  val tags: List<String>?,
  val description: String?,
  val ceRegistry: String?,

  /**
   * The name of the property used to look up the name of this subscription/publisher if
   * different from the top level defined one. Usual in pubsub
   */
  val config: InternalCloudEventProperty?,

  // allows multiple subscribers in config property, comma separated
  val multiSupport: Boolean?,

  // multiple listeners for the same topic, everyone gets a copy?
  val broadcast: Boolean?,

  // prefix name for the subscription if it is a broadcast channel
  val name: String?,

  // property to get the name of the prefix from if name isn't specified and its broadcast
  val prefixConfig: InternalCloudEventProperty?,

  val conditional: InternalCloudEventProperty?,

  // if provided, filter incoming events to these
  val cloudEventsInclude: List<String>?
)

internal data class InternalCloudEventPublisherMultiCast(val property: String, val prefixProperty: String)

internal data class InternalCloudEventPublisherConfig(
  val tags: List<String>?,
  val description: String?,
  val ceRegistry: String?,

  /**
   * The name of the property used to look up the name of this subscription/publisher if
   * different from the top level defined one. Usual in pubsub
   */
  val config: InternalCloudEventProperty?,

  // allows multiple publishers in config property, comma separated
  val multiSupport: Boolean?,

  val conditional: InternalCloudEventProperty?,
  val metricName: InternalCloudEventProperty?,

  val cloudEventsInclude: List<String>?,

  val multiCast: InternalCloudEventPublisherMultiCast?
)

data class CloudEventSubscriberConfig(
  val tags: List<String>,
  val description: String?,
  val ceRegistry: String,
  val channelNames: List<String>,
  val originalConfigProperty: String?,
  val broadcast: Boolean,
  val name: String?,
  val prefix: String,
  val cloudEventsInclude: List<String>,
  val handler: (event: CloudEvent) -> Unit
)

data class CloudEventPublisherMulticast(val config: String, val prefix: String)

data class CloudEventPublisherConfig(
  val name: String,
  val tags: List<String>,
  val description: String?,
  val ceRegistry: String,
  val channelNames: List<String>,
  val originalConfigProperty: String?,
  val metric: String?,
  val cloudEventsInclude: List<String>,
//  val multiCast: CloudEventPublisherMulticast?
)

internal data class InternalCloudEventConfig(
  val description: String?,
  val cloudEvents: List<String>?,

  /**
   * Default property used to determine what the name of the channel when used with streaming
   * like kinesis and nats. Not used by PubSub.
   */
  val channelName: InternalCloudEventProperty?,

  /**
   * If this conditional isn't true, all the config underneath should be ignored.
   */
  val conditional: InternalCloudEventProperty?,
  val subscribers: Map<String, InternalCloudEventSubscriberConfig>?,
  val publishers: Map<String, InternalCloudEventPublisherConfig>?
)

class CloudEventConfig(
  val name: String, val description: String?,
  private val cloudEvents: List<String>, val channelName: String?, val enabled: Boolean,
  val subscribers: List<CloudEventSubscriberConfig>,
  val publishers: List<CloudEventPublisherConfig>,
  private val publisherRegistry: CloudEventPublisherRegistry
) {
  fun registerPublisher(
    publisher: CloudEventPublisherConfig,
    channelName: String,
    compress: Boolean,
    handler: (msg: CloudEvent) -> Unit
  ) {
    val eventTypes = publisher.cloudEventsInclude.ifEmpty { cloudEvents }
    val metric = CloudEventChannelMetric(
      MetricsCollector.counter(publisher.metric ?: "${name}_failure_counter", "Publishing CE types $eventTypes"),
      MetricsCollector.histogram(publisher.metric ?: "${name}_publishing", "Publishing CE types $eventTypes")
    )
    eventTypes.forEach { ceType ->
      log.trace("ce: publishing {} on channel {} for {}", ceType, channelName, name)
      publisherRegistry.registerForPublishing(ceType, metric, compress, handler)
    }
  }

  companion object {
    private val log: Logger = LoggerFactory.getLogger(CloudEventConfig::class.java)
  }
}

interface CloudEventConfigDiscoveryProcessor {
  fun processSubscriber(subscriber: CloudEventSubscriberConfig, config: CloudEventConfig)
  fun processPublisher(publisher: CloudEventPublisherConfig, config: CloudEventConfig)
}

interface CloudEventConfigDiscovery {
  fun discover(type: String, processor: CloudEventConfigDiscoveryProcessor)
}

/**
 * this class is implemented by each messaging layer, and it is responsible for finding its own config files and understanding them
 */
class CloudEventConfigDiscoveryService @Inject
constructor(
  private val publisherRegistry: CloudEventPublisherRegistry,
  private val receiverRegistry: CloudEventReceiverRegistry
) : CloudEventConfigDiscovery {
  private val log: Logger = LoggerFactory.getLogger(CloudEventConfigDiscovery::class.java)
  private val yaml = Yaml()
  private val mapper = ObjectMapper().apply { registerModule(KotlinModule.Builder().build()) }

  companion object {
    private val tags = mutableSetOf<String>()

    /**
     * Applications should call this to indicate who they are and therefore what tagged channels need to be created for
     * them to operate correctly.
     */
    fun addTags(vararg tag: String) {
      tag.forEach { tags.add(it) }
    }
  }

  override fun discover(type: String, processor: CloudEventConfigDiscoveryProcessor) {
    log.info("{}: static configuration started looking for matching tags {}", type, tags)

    // make sure we default pick up the app name
    System.getProperty(Info.APPLICATION_NAME_PROPERTY)?.let { appName ->
      tags.add(appName)
    }

    log.trace("cloud-event discovery: tags are {}", tags)

    val resources =
      CloudEventConfigDiscovery::class.java.classLoader.getResources("META-INF/cloud-events/cloud-events.yaml")

    val discoveries = mutableListOf<CloudEventConfig>()

    while (resources.hasMoreElements()) {
      val resource = resources.nextElement()
      log.trace("ce-config: found cloud-event config at {}", resource.toExternalForm())

      BufferedReader(InputStreamReader(resource.openStream())).use { br ->
        val original: Map<String, Any> = yaml.load(br)
        discoveries.addAll(recode(original, type))
      }
    }

    discoveries.forEach { config ->
      log.trace("{}: processing publishers channel {}, publishers {}", type, config.name, config.publishers.map { it.name })
      config.publishers.forEach { processor.processPublisher(it, config) }
      log.trace("{}: finished publishers channel {}", type,config.name)
    }

    discoveries.forEach { config ->
      log.trace("{}: processing subscribers channel {}, subscribers: {}", type, config.name, config.subscribers.map { it.name })
      config.subscribers.forEach {

        processor.processSubscriber(it, config)
      }
      log.trace("{}: processing subscribers channel {}", type, config.name)
    }

    log.info("{}: static configuration ended", type)
  }

  fun parse(type: String, yml: String): List<CloudEventConfig> {
    return recode(yaml.load(yml), type)
  }

  private fun explode(source: Map<String, Any>, target: MutableMap<String, Any>) {
    for (entry in source) {
      var targetMap = target
      val keySegments = entry.key.split(".")
      // Rebuild the map
      for (keySegment in keySegments.dropLast(1)) {
        if (targetMap.containsKey(keySegment)) {
          val value = targetMap[keySegment]
          if (value is Map<*, *>) {
            targetMap = value as MutableMap<String, Any>
            continue
          }
        }
        val newNestedMap = mutableMapOf<String, Any>()
        targetMap[keySegment] = newNestedMap
        targetMap = newNestedMap
      }
      // Put values into it
      targetMap[keySegments.last()] = entry.value
    }
  }

  private fun flatten(source: Map<String, Any>, target: MutableMap<String, Any>, prefix: String = "") {
    for (entry in source) {
      val fullKey = if (prefix.isNotEmpty()) prefix + "." + entry.key else entry.key
      if (entry.value is Map<*, *>) {
        flatten(entry.value as Map<String, Any>, target, fullKey)
      } else {
        target[fullKey] = entry.value
      }
    }
  }

  private fun recode(config: Map<String, Any>?, type: String): List<CloudEventConfig> {
    // snakeyaml doesn't obey the inheritance, and yaml itself doesn't
    if (config == null) return listOf()

    val squashMap = config[type] as Map<String, Any>? ?: return listOf()

    val squashed = mutableMapOf<String, Any>()
    flatten(config["default"] as Map<String, Any>, squashed)
    flatten(squashMap, squashed)
    val exploded = mutableMapOf<String, Any>()
    explode(squashed, exploded)

    val data: Map<String, InternalCloudEventConfig> =
      mapper.convertValue(exploded, object : TypeReference<Map<String, InternalCloudEventConfig>>() {})
    return recodeData(data)
  }

  private fun recodeData(source: Map<String, InternalCloudEventConfig>?): List<CloudEventConfig> {
    if (source == null) return listOf()

    return source.map { channel ->
      recodeChannel(channel.key, channel.value)
    }
  }

  private fun recodeChannel(name: String, config: InternalCloudEventConfig): CloudEventConfig {
    return CloudEventConfig(
      name, config.description, config.cloudEvents ?: listOf(),
      resolveConfig(config.channelName),
      isEnabled(config.conditional),
      recodeSubscribers(config.subscribers, config),
      recodePublishers(config.publishers, config),
      publisherRegistry
    )
  }

  private fun recodePublishers(
    publishers: Map<String, InternalCloudEventPublisherConfig>?,
    config: InternalCloudEventConfig
  ): List<CloudEventPublisherConfig> {
    if (publishers == null) return listOf()
    return publishers.map { recodePublisher(it.key, it.value, config) }
      .filterNotNull()
      .filter { it.tags.intersect(tags).isNotEmpty() }
  }

  private fun recodePublisher(
    name: String,
    s: InternalCloudEventPublisherConfig,
    config: InternalCloudEventConfig
  ): CloudEventPublisherConfig? {
    if (!isEnabled(s.conditional)) return null
    val configProperty = s.config ?: config.channelName
    val channelNames = if (s.multiCast == null) multiSupport(s.multiSupport, configProperty) else decodeMultiCast(s.multiCast)
    return CloudEventPublisherConfig(
      name,
      s.tags ?: listOf(), s.description, s.ceRegistry ?: "common",
      channelNames,
      configProperty?.property,
      resolveConfig(s.metricName),
      s.cloudEventsInclude ?: listOf(),
    )
  }

  private fun decodeMultiCast(multiCast: InternalCloudEventPublisherMultiCast): List<String> {
    val config = FallbackPropertyConfig.getConfig(multiCast.property)
    if (config == null) return listOf()

    return config.split(",").map { it.trim() }.filter { it.isNotEmpty() }.map {
      FallbackPropertyConfig.getConfig("${multiCast.prefixProperty}.${it}")
    }.filterNotNull()
  }

  private fun recodeSubscribers(
    subscribers: Map<String, InternalCloudEventSubscriberConfig>?,
    config: InternalCloudEventConfig
  ): List<CloudEventSubscriberConfig> {
    if (subscribers == null) return listOf()
    return subscribers.map { recodeSubscriber(it.key, it.value, config) }
      .filterNotNull()
      .filter { it.tags.intersect(tags).isNotEmpty() }
  }

  private fun recodeSubscriber(
    name: String,
    s: InternalCloudEventSubscriberConfig,
    config: InternalCloudEventConfig
  ): CloudEventSubscriberConfig? {

    if (!isEnabled(s.conditional)) {
      log.trace("{} subscriber in channel is not enabled for this profile", name)
      return null
    }
    val ceInclude = s.cloudEventsInclude ?: listOf()
    val ceFilter = ceInclude.ifEmpty { (config.cloudEvents ?: listOf()) }
    val ceRegistry = s.ceRegistry ?: "common"

    val configProperty = s.config ?: config.channelName

    return CloudEventSubscriberConfig(
      s.tags ?: listOf(), s.description, s.ceRegistry ?: "common",
      multiSupport(s.multiSupport, configProperty),
      configProperty?.property,
      s.broadcast == true, name, s.name ?: resolveConfig(s.prefixConfig) ?: name,
      ceInclude
    ) { ce ->
      log.trace("ce: subscription {} compare {} against {}", name, ce.type, ceFilter)
      if (ceFilter.contains(ce.type)) {
        if (ceRegistry != "common") {
          receiverRegistry.registry(ceRegistry).process(ce)
        } else {
          receiverRegistry.process(ce)
        }
      }
    }
  }

  private fun multiSupport(multiSupport: Boolean?, config: InternalCloudEventProperty?): List<String> {
    val conf = resolveConfig(config) ?: return listOf()
    if (multiSupport != true) return listOf(conf)
    return conf.split(",").map { it.trim() }.filter { it.isNotEmpty() }
  }


  private fun isEnabled(conditional: InternalCloudEventProperty?): Boolean {
    if (conditional?.property == null) return true
    // negative has no use case at the moment so it isn't used
    val negative = conditional.property.startsWith("!")
    val cond = if (negative) conditional.property.substring(1) else conditional.property
    val prop = FallbackPropertyConfig.getConfig(cond, conditional.default ?: "")
    return truthy.contains(prop.lowercase())
  }

  private val truthy = listOf("true", "1", "t")

  private fun resolveConfig(config: InternalCloudEventProperty?): String? {
    if (config?.property == null) return null

    return if (config.default == null) {
      FallbackPropertyConfig.getMandatoryConfig(config.property)
    } else {
      FallbackPropertyConfig.getConfig(config.property, config.default)
    }
  }
}
