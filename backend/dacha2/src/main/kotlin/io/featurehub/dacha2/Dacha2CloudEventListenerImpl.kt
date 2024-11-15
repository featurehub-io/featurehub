package io.featurehub.dacha2

import cd.connect.app.config.ConfigKey
import io.featurehub.dacha.model.PublishEnvironment
import io.featurehub.dacha.model.PublishFeatureValues
import io.featurehub.dacha.model.PublishServiceAccount
import io.featurehub.events.CloudEventReceiverRegistry
import io.featurehub.events.EventingConnection
import io.featurehub.lifecycle.LifecyclePriority
import io.featurehub.lifecycle.LifecycleStarted
import io.featurehub.utils.ExecutorSupplier
import io.featurehub.utils.FallbackPropertyConfig
import jakarta.inject.Inject
import org.glassfish.hk2.api.IterableProvider
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.concurrent.ExecutorService


/**
 * We pass in the dacha2 cache directly as
 */
@LifecyclePriority(priority = 10)
class Dacha2CloudEventListenerImpl @Inject constructor(
  private val dacha2Caches: IterableProvider<Dacha2CacheListener>,
  private val dacha2Cache: Dacha2Cache,
  register: CloudEventReceiverRegistry,
  executorSupplier: ExecutorSupplier,
  eventingConnection: EventingConnection
) : LifecycleStarted {
  private val log: Logger = LoggerFactory.getLogger(Dacha2CloudEventListenerImpl::class.java)
  var dacha2CacheList = mutableListOf<Dacha2CacheListener>()
  var nThreads = FallbackPropertyConfig.getConfig("dacha2.thread-processors", "20").toInt()
  val executorService: ExecutorService

  init {
    executorService = executorSupplier.executorService(nThreads!!)

    eventingConnection.registerForConnectionEvents { event ->
      dacha2Cache.enableCache(event == EventingConnection.ConnectionStatus.CONNECTED)
    }

    dacha2Cache.enableCache(eventingConnection.status() == EventingConnection.ConnectionStatus.CONNECTED)

    register.listen(PublishEnvironment::class.java) { env, ce ->
      log.trace("received environment {}", env)
      dacha2Cache.updateEnvironment(env)
      dacha2CacheList.forEach { dl ->
        executorService.submit {
          dl.updateEnvironment(env)
        }
      }
    }

    register.listen(PublishServiceAccount::class.java) { serviceAccount, ce ->
      log.trace("received service account {}", serviceAccount)
      dacha2Cache.updateServiceAccount(serviceAccount)
      dacha2CacheList.forEach { dl ->
        executorService.submit {
          dl.updateServiceAccount(serviceAccount)
        }
      }
    }

    register.listen(PublishFeatureValues::class.java) { features, ce ->
      log.trace("received feature values {}", features)
      for (feature in features.features) {
        dacha2Cache.updateFeature(feature)
        dacha2CacheList.forEach { dl ->
          executorService.submit {
            dl.updateFeature(feature)
          }
        }
      }
    }
  }

  override fun started() {
    dacha2CacheList.addAll(dacha2Caches)
  }
}
