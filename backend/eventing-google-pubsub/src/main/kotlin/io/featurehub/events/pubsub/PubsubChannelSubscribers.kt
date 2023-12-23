package io.featurehub.mr.events.listeners

import cd.connect.app.config.ConfigKey
import cd.connect.app.config.DeclaredConfigResolver
import io.featurehub.events.CloudEventReceiverRegistry
import io.featurehub.events.pubsub.PubSubFactory
import io.featurehub.lifecycle.LifecyclePriority
import io.featurehub.lifecycle.LifecycleShutdown
import io.featurehub.lifecycle.LifecycleStarted
import jakarta.inject.Inject
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class RetryException : RuntimeException() {
}

/**
 * This listens to all channels that are listed here. They all go into the big bucket of CloudEventReceiverRegistry. This should happen after all
 * the application events finish so it starts listening LATE so everything else is ready
 */
@LifecyclePriority(priority = LifecyclePriority.APPLICATION_PRIORITY_END)
class PubsubChannelSubscribers @Inject constructor(private val pubSubFactory: PubSubFactory, private val registry: CloudEventReceiverRegistry):
  LifecycleStarted, LifecycleShutdown {
  @ConfigKey("cloudevents.inbound.channel-names")
  var subscriptions: List<String>? = listOf()

  private val log: Logger = LoggerFactory.getLogger(PubsubChannelSubscribers::class.java)

  override fun started() {
    DeclaredConfigResolver.resolve(this)

    subscriptions?.let { subs ->
      val subscriptions = subs
        .map { it.trim() }
        .filter { it.isNotEmpty() }

      log.info("cloudevents: inbound channels {}", subscriptions)

      subscriptions
        .forEach {
          pubSubFactory.makeSubscriber(it) { msg ->
            var returnVal = true

            try {
              registry.process(msg)
            } catch (r: RetryException) {
              returnVal = false
            } catch (e: Exception) {
              log.error("Unable to process event", e)
              log.trace("Failed event {}", msg)
            }

            returnVal
          }
        }
    }

    pubSubFactory.start()
  }

  override fun shutdown() {
    pubSubFactory.shutdown()
  }
}


