package io.featurehub.events

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
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
  val negativeTags: List<String>,
  val description: String?,
  val ceRegistry: String,
  var channelNames: List<String>,
  val originalConfigProperty: String?,
  val broadcast: Boolean,
  val name: String,
  val prefix: String,
  var cloudEventsInclude: List<String>,
  var handler: (event: CloudEvent) -> Unit
)


data class CloudEventPublisherConfig(
  val name: String,
  val tags: List<String>,
  val negativeTags: List<String>,
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
  val cloudEvents: List<String>, val channelName: String?, val enabled: Boolean,
  val subscribers: MutableList<CloudEventSubscriberConfig>,
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

internal data class SubscriberMergeConfig(val config: CloudEventConfig, var subscriber: CloudEventSubscriberConfig)

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
  private val mapper = ObjectMapper().apply { registerModule(KotlinModule.Builder().build()).registerModule(
    JavaTimeModule()
  ) }

  companion object {
    private val tags = mutableSetOf<String>()

    /**
     * Applications should call this to indicate who they are and therefore what tagged channels need to be created for
     * them to operate correctly.
     */
    fun addTags(vararg tag: String) {
      tag.forEach { tags.add(it) }
    }

    // only used in testing
    fun clearTags() {
      tags.clear()
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
      if (config.publishers.isNotEmpty()) {
        log.trace("{}: processing publishers channel {}, publishers {}", type, config.name, config.publishers.map { it.name })
        config.publishers.forEach { processor.processPublisher(it, config) }
        log.trace("{}: finished publishers channel {}", type,config.name)
      } else {
        log.trace("{}: channel {} not used for publishing", type, config.name)
      }
    }

    discoveries.forEach { config ->
      if (config.subscribers.isNotEmpty()) {
          log.trace("{}: processing subscribers channel {}, subscribers: {}", type, config.name, config.subscribers.map { it.name })
        config.subscribers.forEach {

          processor.processSubscriber(it, config)
        }
        log.trace("{}: processing subscribers channel {}", type, config.name)
      } else {
        log.trace("{}: channel {} not used for subscribers", type, config.name)
      }
    }

    log.info("{}: static configuration ended", type)
  }

  fun parse(type: String, yml: String): List<CloudEventConfig> {
    return recode(yaml.load(yml), type)
  }

  private fun explode(source: Map<String, Any>, target: MutableMap<String, Any>): MutableMap<String, Any> {
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

    return target
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

    val exploded = if (config.containsKey("default")) {
      val squashed = mutableMapOf<String, Any>()
      flatten(config["default"] as Map<String, Any>, squashed)
      flatten(squashMap, squashed)

      explode(squashed, mutableMapOf())
    } else squashMap

    val data: Map<String, InternalCloudEventConfig> =
      mapper.convertValue(exploded, object : TypeReference<Map<String, InternalCloudEventConfig>>() {})
    return recodeData(data)
  }

  private fun recodeData(source: Map<String, InternalCloudEventConfig>?): List<CloudEventConfig> {
    if (source == null) return listOf()

    val channels = source.map { channel ->
      recodeChannel(channel.key, channel.value)
    }

    // now we have subscribers only for this service, if several of them share the same config property, they should be turned
    // into a single subscriber with multiple acceptable cloud events and potentially multiple subscribers (if they were multiSupports)
    // you can have a service that simply goes cloud.events.inbound=x,y,z for example but those are logically events from different
    // services and so will all be coming in from different channels. This only works for non-broadcast events and events


    val mergeConfigs = mutableMapOf<String,SubscriberMergeConfig>()
    channels.forEach { channel ->
      val removeSubscribers = mutableListOf<CloudEventSubscriberConfig>()
      channel.subscribers.forEach { sub ->
        if (sub.originalConfigProperty != null) {
          // is this config property already used somewhere else? if so, we need to combine them
          if (mergeConfigs.containsKey(sub.originalConfigProperty)) {
            val originalConfig = mergeConfigs[sub.originalConfigProperty]!!.config
            val original = mergeConfigs[sub.originalConfigProperty]!!.subscriber
            val name = sub.name // should really be an array :-(
            val ceFilter =
              ((sub.cloudEventsInclude.ifEmpty { channel.cloudEvents }) +
              (original.cloudEventsInclude.ifEmpty { originalConfig.cloudEvents })).distinct()
            val ceRegistry = sub.ceRegistry

            original.channelNames = (original.channelNames + sub.channelNames).distinct()
            original.cloudEventsInclude = ceFilter
            original.handler = { ce ->
              log.trace("ce: subscription {} compare {} against {}", name, ce.type, ceFilter)
              if (ceFilter.contains(ce.type)) {
                if (ceRegistry != "common") {
                  receiverRegistry.registry(ceRegistry).process(ce)
                } else {
                  receiverRegistry.process(ce)
                }
              } else {
                log.trace("ce: type {} not in filter on channel {}, ignored", ce.type, name)
              }
            }
//            = CloudEventSubscriberConfig(
//              (original.tags + sub.tags).distinct(),
//              sub.description ?: original.description,
//              sub.ceRegistry,
//              ,
//              sub.originalConfigProperty,
//              sub.broadcast || original.broadcast,
//              name,
//              sub.prefix,
//              ceFilter


            removeSubscribers.add(sub)
          } else {
            mergeConfigs[sub.originalConfigProperty] = SubscriberMergeConfig(channel, sub)
          }
        }

      }

      channel.subscribers.removeAll(removeSubscribers)
    }


    return channels
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
      .filter { it.negativeTags.intersect(tags).isEmpty() && it.tags.intersect(tags).isNotEmpty() }
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
      positiveTags(s.tags), negativeTags(s.tags), s.description, s.ceRegistry ?: "common",
      channelNames,
      configProperty?.property,
      resolveConfig(s.metricName),
      s.cloudEventsInclude ?: listOf(),
    )
  }

  private fun positiveTags(tags: List<String>?): List<String> {
    if (tags == null) return listOf()
    return tags.filter { !it.startsWith("!") }
  }

  private fun negativeTags(tags: List<String>?): List<String> {
    if (tags == null) return listOf()
    return tags.filter { it.startsWith("!") }.map { it.substring(1) }
  }

  private fun decodeMultiCast(multiCast: InternalCloudEventPublisherMultiCast): List<String> {
    val config = FallbackPropertyConfig.getConfig(multiCast.property) ?: return listOf()

    return config.split(",").map { it.trim() }.filter { it.isNotEmpty() }.mapNotNull {
      FallbackPropertyConfig.getConfig("${multiCast.prefixProperty}.${it}")
    }
  }

  private fun recodeSubscribers(
    subscribers: Map<String, InternalCloudEventSubscriberConfig>?,
    config: InternalCloudEventConfig
  ): MutableList<CloudEventSubscriberConfig> {
    if (subscribers == null) return mutableListOf()
    val subscribers = subscribers.map { recodeSubscriber(it.key, it.value, config) }
      .filterNotNull()
      .filter { it.negativeTags.intersect(tags).isEmpty() && it.tags.intersect(tags).isNotEmpty() }.toMutableList()

    return subscribers
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
      positiveTags(s.tags), negativeTags(s.tags), s.description, s.ceRegistry ?: "common",
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
    // negative has no use case at the moment, so it isn't used
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
