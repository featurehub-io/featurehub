package io.featurehub.dacha2

import cd.connect.app.config.ConfigKey
import io.featurehub.dacha.model.PublishEnvironment
import io.featurehub.dacha.model.PublishFeatureValues
import io.featurehub.dacha.model.PublishServiceAccount
import io.featurehub.events.CloudEventReceiverRegistry
import io.featurehub.utils.ExecutorSupplier
import jakarta.annotation.PostConstruct
import jakarta.inject.Inject
import org.glassfish.hk2.api.IterableProvider
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.concurrent.ExecutorService


/**
 * We pass in the dacha2 cache directly as
 */
class Dacha2CloudEventListenerImpl @Inject constructor(
  private val dacha2Caches: IterableProvider<Dacha2CacheListener>,
  private val dacha2Cache: Dacha2Cache,
  register: CloudEventReceiverRegistry,
  executorSupplier: ExecutorSupplier
) {
  private val log: Logger = LoggerFactory.getLogger(Dacha2CloudEventListenerImpl::class.java)
  var dacha2CacheList = mutableListOf<Dacha2CacheListener>()
  @ConfigKey("dacha2.thread-processors")
  var nThreads: Int? = 20
  val executorService: ExecutorService

  init {
    executorService = executorSupplier.executorService(nThreads!!)
    register.listen(PublishEnvironment::class.java) { env ->
      log.trace("received environment {}", env)
      dacha2Cache.updateEnvironment(env)
      dacha2CacheList.forEach { dl ->
        executorService.submit {
          dl.updateEnvironment(env)
        }
      }
    }

    register.listen(PublishServiceAccount::class.java) { serviceAccount ->
      log.trace("received service account {}", serviceAccount)
      dacha2Cache.updateServiceAccount(serviceAccount)
      dacha2CacheList.forEach { dl ->
        executorService.submit {
          dl.updateServiceAccount(serviceAccount)
        }
      }
    }

    register.listen(PublishFeatureValues::class.java) { features ->
      log.trace("received feature values {}", features)
      for (feature in features.features) {
        dacha2Cache?.updateFeature(feature)
        dacha2CacheList.forEach { dl ->
          executorService.submit {
            dl.updateFeature(feature)
          }
        }
      }
    }
  }

  @PostConstruct
  fun init() {
    dacha2CacheList.addAll(dacha2Caches)
  }
}
