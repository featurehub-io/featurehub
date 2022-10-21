package io.featurehub.dacha2

import io.featurehub.dacha.model.PublishEnvironment
import io.featurehub.dacha.model.PublishFeatureValues
import io.featurehub.dacha.model.PublishServiceAccount
import io.featurehub.events.CloudEventReceiverRegistry
import jakarta.annotation.PostConstruct
import jakarta.inject.Inject
import org.glassfish.hk2.api.IterableProvider
import org.slf4j.Logger
import org.slf4j.LoggerFactory


class Dacha2CloudEventListenerImpl @Inject constructor(
  private val dacha2Caches: IterableProvider<Dacha2CacheListener>,
  register: CloudEventReceiverRegistry
) {
  private val log: Logger = LoggerFactory.getLogger(Dacha2CloudEventListenerImpl::class.java)
  var dacha2CacheList = mutableListOf<Dacha2CacheListener>()

  init {
    register.listen(PublishEnvironment::class.java) { env ->
      log.trace("received environment {}", env)
      dacha2CacheList.forEach { it.updateEnvironment(env) }
    }

    register.listen(PublishServiceAccount::class.java) { serviceAccount ->
      log.trace("received service account {}", serviceAccount)
      dacha2CacheList.forEach { it.updateServiceAccount(serviceAccount) }
    }

    register.listen(PublishFeatureValues::class.java) { features ->
      log.trace("received feature values {}", features)
      for (feature in features.features) {
        dacha2CacheList.forEach { it.updateFeature(feature) }
      }
    }
  }

  @PostConstruct
  fun init() {
    dacha2CacheList.addAll(dacha2Caches)
  }
}
