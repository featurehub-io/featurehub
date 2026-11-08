package io.featurehub.redis

import java.util.UUID

/**
 * The key schema shared between the writer (MR) and reader (Dacha2).
 *
 * The prefix is wrapped in a hash tag (`{prefix}`) so that in cluster mode all keys for one FeatureHub install land
 * in the same slot - this allows MULTI transactions across a service account and its api-key indexes.
 */
class RedisKeys(prefix: String) {
  private val base = "{${prefix}}:"

  fun environment(envId: UUID) = "${base}env:${envId}"
  fun serviceAccount(serviceAccountId: UUID) = "${base}sa:${serviceAccountId}"
  fun serviceAccountKey(apiKey: String) = "${base}sakey:${apiKey}"

  val metaSync = "${base}meta:sync"
  val syncLock = "${base}meta:lock:sync"

  val environmentPattern = "${base}env:*"
  val serviceAccountPattern = "${base}sa:*"
  val serviceAccountKeyPattern = "${base}sakey:*"

  fun environmentIdFromKey(key: String): UUID? = idFrom(key, "${base}env:")
  fun serviceAccountIdFromKey(key: String): UUID? = idFrom(key, "${base}sa:")

  private fun idFrom(key: String, start: String): UUID? {
    if (!key.startsWith(start)) return null
    return try {
      UUID.fromString(key.substring(start.length))
    } catch (e: IllegalArgumentException) {
      null
    }
  }
}
