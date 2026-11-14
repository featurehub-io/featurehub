package io.featurehub.mr.events.common

import io.featurehub.dacha.model.CacheServiceAccount
import io.featurehub.dacha.model.PublishEnvironment
import java.util.UUID

/**
 * Gives access to the publishable (i.e. not archived, not unpublished, not in an archived organisation) cache
 * objects directly from the database. Used by cache stores that keep whole objects (e.g. Redis) and need to
 * re-read the current state rather than apply deltas.
 *
 * These calls are synchronous and hit the database. In a multi-tenant deployment they must be called on threads
 * that have no tenant set so that they see all tenants.
 */
interface CacheWalker {
  fun publishableEnvironmentIds(): List<UUID>
  fun publishableServiceAccountIds(): List<UUID>

  /**
   * returns null if the environment does not exist or is no longer publishable
   */
  fun publishableEnvironment(id: UUID): PublishEnvironment?

  /**
   * returns null if the service account does not exist or is no longer publishable
   */
  fun publishableServiceAccount(id: UUID): CacheServiceAccount?
}

/**
 * A broadcaster which manages its own "publish everything" process. It is excluded from the fan-out when the
 * whole cache is published (e.g. the cache refresh "allTheThings") and instead told that
 * a full publish was requested so it can do it in its own way.
 */
interface SelfSyncingCacheBroadcast : CacheBroadcast {
  fun fullPublishRequested()
}
