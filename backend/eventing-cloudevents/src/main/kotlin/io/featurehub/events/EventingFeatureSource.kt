package io.featurehub.events

import jakarta.inject.Inject
import jakarta.ws.rs.core.Feature
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.yaml.snakeyaml.Yaml
import java.io.BufferedReader
import java.io.InputStreamReader

interface EventingFeatureSource {
  val featureSource: Feature?
}

interface CloudEventProperty {
  val description: String?

  /**
   * The env var or system property which stores the config
   */
  val property: String?

  /**
   * The default value if one isn't specified
   */
  val default: Boolean?
}

interface CloudEventCommonConfig {
  val tags: List<String>?
  val description: String?
  val ceRegistry: String?
  /**
   * The name of the property used to look up the name of this subscription/publisher if
   * different from the top level defined one. Usual in pubsub
   */
  val config: CloudEventProperty?
}

interface CloudEventSubscriberConfig : CloudEventCommonConfig {
  // multiple listeners for the same topic, everyone gets a copy?
  val broadcast: Boolean?

  // prefix name for the subscription if it is a broadcast channel
  val name: String?
  // property to get the name of the prefix from if name isn't specified and its broadcast
  val prefixConfig: CloudEventProperty?


  val conditional: CloudEventProperty?
  // if provided, filter incoming events to these
  val cloudEventsInclude: List<String>?
}

interface CloudEventPublisherConfig : CloudEventCommonConfig {
}

interface CloudEventConfig {
  val description: String?
  val cloudEvents: List<String>

  /**
   * Default property used to determine what the name of the channel when used with streaming
   * like kinesis and nats. Not used by PubSub.
   */
  val config: CloudEventProperty?

  /**
   * If this conditional isn't true, all of the config underneath should be ignored.
   */
  val conditional: CloudEventProperty?
  val subscribers: Map<String, CloudEventSubscriberConfig>
  val publishers: Map<String, CloudEventPublisher>
}

interface CloudEventSource {
  val nats: Map<String, CloudEventConfig>
  val kinesis: Map<String, CloudEventConfig>
  val pubsub: Map<String, CloudEventConfig>
}


/**
 * this class is implemented by each messaging layer and it is responsible for finding its own config files and understanding them
 */
open class CloudEventProviderBase {
  private val log: Logger = LoggerFactory.getLogger(CloudEventProviderBase::class.java)

  fun getConfig(callback: (CloudEventSource) -> Unit ) {
    val resources = this.javaClass.classLoader.getResources("META-INF/cloud-events/cloud-events.yaml")

    while (resources.hasMoreElements()) {
      val resource = resources.nextElement()
      log.debug("found cloud-event config at {}", resource.toExternalForm())
      callback(Yaml().load(BufferedReader(InputStreamReader(resource.openStream()))))
    }
  }
}
