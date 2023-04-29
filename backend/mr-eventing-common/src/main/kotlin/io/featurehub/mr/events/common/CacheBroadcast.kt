package io.featurehub.mr.events.common

import io.featurehub.dacha.model.PublishEnvironment
import io.featurehub.dacha.model.PublishFeatureValues
import io.featurehub.dacha.model.PublishServiceAccount

interface CacheBroadcast {
    fun publishEnvironment(eci: PublishEnvironment)
    fun publishServiceAccount(saci: PublishServiceAccount)
    fun publishFeatures(features: PublishFeatureValues)
}
