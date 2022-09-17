package io.featurehub.mr.events.common.listeners


/**
 * Allows overriding of how the edge update listener callback is created
 */
interface FeatureUpdateFactory {
  fun createListener(): FeatureUpdateListener
}
