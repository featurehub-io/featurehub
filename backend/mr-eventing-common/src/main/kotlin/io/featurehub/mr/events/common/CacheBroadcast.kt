package io.featurehub.mr.events.common

import io.featurehub.dacha.model.PublishEnvironment
import io.featurehub.dacha.model.PublishFeatureValues
import io.featurehub.dacha.model.PublishServiceAccount

interface CacheBroadcast {
    fun publishEnvironment(cacheName: String, eci: PublishEnvironment)
    fun publishServiceAccount(cacheName: String, saci: PublishServiceAccount)
    fun publishFeatures(cacheName: String, features: PublishFeatureValues)
}
