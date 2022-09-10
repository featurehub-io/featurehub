package io.featurehub.mr.events.common.listeners

import io.featurehub.mr.messaging.StreamedFeatureUpdate

interface EdgeUpdateListener {
  fun processUpdate(update: StreamedFeatureUpdate)
}
