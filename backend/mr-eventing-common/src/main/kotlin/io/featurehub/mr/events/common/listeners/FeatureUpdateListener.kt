package io.featurehub.mr.events.common.listeners

import io.featurehub.mr.messaging.StreamedFeatureUpdate

interface FeatureUpdateListener {
  fun processUpdate(update: StreamedFeatureUpdate)
}
