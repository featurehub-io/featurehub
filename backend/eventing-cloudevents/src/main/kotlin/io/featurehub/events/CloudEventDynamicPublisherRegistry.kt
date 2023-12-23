package io.featurehub.events

import io.featurehub.metrics.MetricsCollector
import org.slf4j.Logger
import org.slf4j.LoggerFactory

interface CloudEventDynamicPublisherRegistry {
  /**
   * This is used by something that wants to publish an event. It passes in the list of prefixes that it supports to create
   * publishers for and when the thing wants to configure it, it will pass the destination which the implementation will discover
   * a publisher for and it will then call it back to allow it to create its publisher.
   *
   * This is called by the PROVIDER of the networking infrastructure.
   */
  fun registerDymamicPublisherProvider(prefixes: List<String>, callback: (params: Map<String, String>,
                                                                          cloudEventType: String, destination: String,
                                                                          destSuffix: String,
                                      metric: CloudEventChannelMetric
    ) -> Unit);

  /**
   * This is called by the code requiring a publisher.
   */
  fun requireDynamicPublisher(destination: String, params: Map<String, String>, cloudEventType: String): Boolean

  /**
   * Depending on which platform we are using for messaging, this will get set. It could be null if none is being used
   * (e.g. the REST only platform)
   */
  fun setDefaultPublisherProvider(prefix: String)

}

class CloudEventDynamicPublisherRegistryImpl : CloudEventDynamicPublisherRegistry {
  private val log: Logger = LoggerFactory.getLogger(CloudEventDynamicPublisherRegistryImpl::class.java)
  private val dynamicPublishers: MutableMap<String, (params: Map<String, String>, cloudEventType: String, destination: String, destSuffix: String, metric: CloudEventChannelMetric) -> Unit> = mutableMapOf()
  private var defaultPublisher: String? = null
  private var counter = 1

  override fun registerDymamicPublisherProvider(
    prefixes: List<String>,
    callback: (params: Map<String, String>, cloudEventType: String, destination: String, destSuffix: String, metric: CloudEventChannelMetric) -> Unit
  ) {
    prefixes.forEach {
      dynamicPublishers[it] = callback
    }
  }

  override fun requireDynamicPublisher(destination: String, params: Map<String, String>, cloudEventType: String): Boolean {
    val pos = destination.indexOf("//")

    if (pos > 0) {
      val type = destination.substring(0, pos + 1)
      dynamicPublishers[type]?.let {
        it(params, cloudEventType, destination, destination.substring(pos+2), makeMetric(params, destination))
        return true
      }
    } else if (defaultPublisher != null && dynamicPublishers.containsKey(defaultPublisher)) {
      dynamicPublishers[defaultPublisher]?.let {
        it(params, cloudEventType, destination, destination.substring(pos+2), makeMetric(params, destination))
        return true
      }
    } else {
      log.error("Unable to register destination {}, no publisher found", destination)
    }

    return false
  }

  private fun makeMetric(params: Map<String, String>, destination: String): CloudEventChannelMetric {
    val dynamicSuffix = (counter ++)

    return CloudEventChannelMetric(
      MetricsCollector.counter(params["metric.fail.name"] ?: "dynamic_counter${dynamicSuffix}",
        params["metric.fail.desc"] ?: "Failures when trying to publish to ${destination}"),
      MetricsCollector.histogram(params["metric.histogram.name"] ?: "dynamic_histogram${dynamicSuffix}",
        params["metric.histogram.desc"] ?: "Updates to ${destination}"))
  }

  override fun setDefaultPublisherProvider(prefix: String) {
    defaultPublisher = prefix
  }
}
