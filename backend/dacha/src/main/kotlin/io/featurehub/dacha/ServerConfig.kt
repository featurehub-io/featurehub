package io.featurehub.dacha

import cd.connect.app.config.ConfigKey
import cd.connect.app.config.DeclaredConfigResolver
import cd.connect.lifecycle.ApplicationLifecycleManager
import cd.connect.lifecycle.LifecycleStatus
import cd.connect.lifecycle.LifecycleTransition
import com.fasterxml.jackson.core.JsonProcessingException
import io.featurehub.dacha.model.PublishEnvironment
import io.featurehub.dacha.model.PublishFeatureValue
import io.featurehub.dacha.model.PublishServiceAccount
import io.featurehub.dacha.resource.DachaEdgeNATSAdapter
import io.featurehub.enriched.model.EnricherPing
import io.featurehub.enricher.EnricherConfig
import io.featurehub.enricher.FeatureEnricher
import io.featurehub.events.CloudEventReceiverRegistry
import io.featurehub.events.nats.NatsCloudEventQueueListener
import io.featurehub.jersey.config.CacheJsonMapper
import io.featurehub.metrics.MetricsCollector
import io.featurehub.mr.model.DachaNATSRequest
import io.featurehub.publish.ChannelConstants
import io.featurehub.publish.ChannelNames
import io.featurehub.publish.NATSSource
import io.nats.client.Message
import io.nats.client.MessageHandler
import jakarta.inject.Inject
import org.glassfish.hk2.api.IterableProvider
import org.slf4j.LoggerFactory
import kotlin.system.exitProcess

class ServerConfig @Inject constructor(
  caches: IterableProvider<CacheUpdateListener>,
  private val natsServer: NATSSource, edgeNatsAdapter: DachaEdgeNATSAdapter,
  private val internalCache: InternalCache,
  private val featureEnricher: FeatureEnricher,
  private val cloudEventsRegistry: CloudEventReceiverRegistry
) {
  private val caches: MutableList<CacheUpdateListener> = ArrayList()
  private val edgeNatsAdapter: DachaEdgeNATSAdapter

  @ConfigKey("cache.name")
  var name = ChannelConstants.DEFAULT_CACHE_NAME

  // only NATS is available for dacha1 anyway
  @ConfigKey("dacha1.inbound.nats.channel-name")
  var ceChannel: String? = "featurehub-dacha1-cloudevents";

  private val shutdownSubscriptionRunners: MutableList<Runnable> = ArrayList()
  private val serviceAccountCounter = MetricsCollector.counter(
    "dacha_service_account_msg_counter", "Service Account Messages received"
  )
  private val featureCounter = MetricsCollector.counter(
    "dacha_feature_msg_counter",
    "Feature Messages received"
  )
  private val environmentsCounter = MetricsCollector.counter(
    "dacha_environments_msg_counter",
    "Environment Messages received"
  )
  private val publishCounter = MetricsCollector.counter(
    "dacha_publish_msg_counter",
    "Publish Messages received"
  )

  init {
    this.caches.addAll(caches);
    this.edgeNatsAdapter = edgeNatsAdapter

    DeclaredConfigResolver.resolve(this)

    listenForEnvironments()
    listenForFeatureValues()
    listenForServiceAccounts()
    listenForFeatureRequests()

    listenForCloudEvents()

    if (EnricherConfig.enabled()) {
      listenAsQueueForFeatureRequestsForEnrichment()
    }

    ApplicationLifecycleManager.registerListener { trans: LifecycleTransition ->
      if (trans.next == LifecycleStatus.TERMINATING) {
        shutdown()
      }
    }
  }

  fun shutdown() {
    shutdownSubscriptionRunners.forEach { obj: Runnable -> obj.run() }
    shutdownSubscriptionRunners.clear()
  }

  fun name(): String {
    return name
  }

  private fun listen(handler: MessageHandler, subject: String) {
    try {
      log.info("listening to subject {}", subject)
      val dispatcher = natsServer.connection.createDispatcher(handler)
      val subscribe = dispatcher.subscribe(subject)
      shutdownSubscriptionRunners.add(
        Runnable { subscribe.unsubscribe(subject) })
    } catch (e: Exception) {
      log.error("Failed to subscribe to subject {}", subject, e)
      exitProcess(-1)
    }
  }

  private fun listenForCloudEvents() {
    val cloudeventListener = NatsCloudEventQueueListener(natsServer, ceChannel!!, "dacha1-queue") { ce ->
      cloudEventsRegistry.process(ce) // the standard logic should pick up any CE messages that a Dacha is happy to process
    }

    cloudEventsRegistry.listen(EnricherPing::class.java) { ep, ce ->
      featureEnricher.enricherPing(ce, ep)
    }

    shutdownSubscriptionRunners.add(
      Runnable { cloudeventListener.close() }
    )
  }

  private fun listenForServiceAccounts() {
    listen(
      { message: Message ->
        try {
          serviceAccountCounter.inc()
          val sa = CacheJsonMapper.readFromZipBytes(message.data, PublishServiceAccount::class.java)
          log.trace("service account received {}", sa)
          internalCache.updateServiceAccount(sa)
          caches.parallelStream().forEach { c: CacheUpdateListener -> c.updateServiceAccount(sa) }
        } catch (e: Exception) {
          log.error("Unable to read message on SA channel", e)
        }
      },
      ChannelNames.serviceAccountChannel(name)
    )
  }

  /**
   * if called, we want to listen to the feature stream again but as a queue
   */
  private fun listenAsQueueForFeatureRequestsForEnrichment() {
    log.info("enricher: listening for feature updates on queue")
    val dispatcher = natsServer.connection.createDispatcher { message ->
      val fv = CacheJsonMapper.readFromZipBytes(message.data, PublishFeatureValue::class.java)
      featureEnricher.processFeature(fv)
    }

    val subject = ChannelNames.featureValueChannel(name)
    val subscribe = dispatcher.subscribe(subject, "enricher-queue")

    shutdownSubscriptionRunners.add(
      Runnable { subscribe.unsubscribe(subject) })
  }

  private fun listenForFeatureValues() {
    listen(
      { message: Message ->
        try {
          featureCounter.inc()
          val fv = CacheJsonMapper.readFromZipBytes(message.data, PublishFeatureValue::class.java)
          log.trace("feature value received {}", fv)
          internalCache.updateFeatureValue(fv)
          caches.parallelStream().forEach { c: CacheUpdateListener -> c.updateFeatureValue(fv) }
        } catch (e: Exception) {
          log.error("Failure to decode feature value message", e)
        }
      },
      ChannelNames.featureValueChannel(name)
    )
  }

  private fun listenForEnvironments() {
    listen(
      { message: Message ->
        try {
          environmentsCounter.inc()
          val e = CacheJsonMapper.readFromZipBytes(message.data, PublishEnvironment::class.java)
          log.trace("environment received {}", e)
          internalCache.updateEnvironment(e)
          caches.parallelStream().forEach { c: CacheUpdateListener -> c.updateEnvironment(e) }
        } catch (ex: Exception) {
          log.error("unable to decode message on environment channel", ex)
        }
      },
      ChannelNames.environmentChannel(name)
    )
  }

  private fun listenForFeatureRequests() {
    val connection = natsServer.connection
    val dispatcher = connection.createDispatcher { msg: Message ->
      try {
        publishCounter.inc()
        val req = CacheJsonMapper.readFromZipBytes(msg.data, DachaNATSRequest::class.java)
        log.trace("received NATs Edge request {}", req)
        val response = edgeNatsAdapter.edgeRequest(req)
        log.trace("responded with NATs Edge request {}", response)
        connection.publish(msg.replyTo, CacheJsonMapper.writeAsZipBytes(response))
      } catch (e: Exception) {
        log.error("Unable to write response to feature request", e)
      }
    }
    val subject = ChannelNames.cache(name, ChannelConstants.EDGE_CACHE_CHANNEL)
    val subscribe = dispatcher.subscribe(subject)
    shutdownSubscriptionRunners.add(
      Runnable { subscribe.unsubscribe(subject) })
  }

  @Throws(JsonProcessingException::class)
  private fun encode(o: Any): ByteArray {
    return CacheJsonMapper.mapper.writeValueAsBytes(o)
  }

  fun publish(subject: String?, o: Any, errorMessage: String?) {
    try {
      //      log.debug("publishing: {} => {} ", subject, o);
      natsServer.connection.publish(subject, encode(o))
    } catch (e: JsonProcessingException) {
      log.error(errorMessage, e)
    }
  }

  companion object {
    private val log = LoggerFactory.getLogger(ServerConfig::class.java)
  }
}
