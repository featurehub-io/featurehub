package io.featurehub.events.kinesis

import cd.connect.app.config.ConfigKey
import cd.connect.app.config.DeclaredConfigResolver
import io.featurehub.events.CloudEventReceiverRegistry
import io.featurehub.lifecycle.LifecycleListener
import io.featurehub.lifecycle.LifecyclePriority
import jakarta.inject.Inject
import org.slf4j.Logger
import org.slf4j.LoggerFactory

// an application has to bind one of these so we know what to name it
interface KinesisAppNameProvider {
  fun name(): String
}

@LifecyclePriority(priority = 12)
class KinesisListenAllTheThings @Inject constructor(kinesisFactory: KinesisFactory, registry: CloudEventReceiverRegistry, nameProvider: KinesisAppNameProvider): LifecycleListener {
  @ConfigKey("cloudevents.inbound.stream-names")
  var subscriptions: List<String>? = listOf()

  private val log: Logger = LoggerFactory.getLogger(KinesisListenAllTheThings::class.java)

  init {
    DeclaredConfigResolver.resolve(this)

    subscriptions?.let { subs ->
      val subscriptions = subs
        .map { it.trim() }
        .filter { it.isNotEmpty() }

      log.info("cloudevents: inbound channels {}", subscriptions)

      subscriptions
        .forEach {
          kinesisFactory.makeSubscriber(nameProvider.name(), it) { msg ->
            try {
              registry.process(msg)
            } catch (e: Exception) {
              log.error("Unable to process event", e)
              log.trace("Failed event {}", msg)
            }
          }
        }
    }
  }
}
