package io.featurehub.mr.events.redis

import io.featurehub.dacha.model.PublishAction
import io.featurehub.dacha.model.PublishEnvironment
import io.featurehub.dacha.model.PublishFeatureValues
import io.featurehub.dacha.model.PublishServiceAccount
import io.featurehub.mr.events.common.SelfSyncingCacheBroadcast
import jakarta.inject.Inject

/**
 * Redis only stores whole environments and service accounts. Every event is therefore turned into a (debounced)
 * refresh of the whole object from the database - including feature changes, which cause the whole environment
 * to be rewritten. This is a known trade-off: Redis is only read when MR and its database are unavailable.
 */
class RedisCacheBroadcaster @Inject constructor(
  private val writer: RedisCacheWriter,
  private val fullSync: RedisFullSync
) : SelfSyncingCacheBroadcast {
  override fun publishEnvironment(eci: PublishEnvironment) {
    if (eci.action == PublishAction.EMPTY) return
    eci.environment?.id?.let { writer.scheduleEnvironment(it) }
  }

  override fun publishServiceAccount(saci: PublishServiceAccount) {
    if (saci.action == PublishAction.EMPTY) return
    saci.serviceAccount?.id?.let { writer.scheduleServiceAccount(it) }
  }

  override fun publishFeatures(features: PublishFeatureValues) {
    features.features.mapNotNull { it.environmentId }.distinct().forEach { writer.scheduleEnvironment(it) }
  }

  override fun fullPublishRequested() {
    fullSync.requestSync("full-publish")
  }
}
