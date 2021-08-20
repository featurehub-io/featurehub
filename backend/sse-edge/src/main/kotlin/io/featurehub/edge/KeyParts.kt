package io.featurehub.edge

import java.util.*

class KeyParts(val cacheName: String, val environmentId: UUID, val serviceKey: String) {

  // we try and collect this data at creation time because it can get removed or deleted by
  // the environment owner at any point, meaning we would have to query it from the database
  // it also means if it drops into something like kafka or NATs Streaming or Pulsar then
  // it is re-playable
  var organisationId: UUID? = null
  var portfolioId: UUID? = null
  var applicationId: UUID? = null
  var serviceKeyId: UUID? = null

  companion object {
    fun fromString(apiUrl: String): KeyParts? {
      val parts: List<String> = apiUrl.split("/")
      if (parts.size == 3) {
        try {
          return KeyParts(parts[0], UUID.fromString(parts[1]), parts[2])
        } catch (e: Exception) {
          return null
        }
      }

      return null
    }
  }

  override fun equals(other: Any?): Boolean {
    if (other is KeyParts) {
      return other.cacheName.equals(cacheName) && other.environmentId.equals(environmentId) && other.serviceKey.equals(serviceKey)
    }

    return false
  }

  override fun hashCode(): Int {
    return String.format("%s/%s/%s", cacheName, environmentId, serviceKey).hashCode()
  }

  override fun toString(): String {
    return "$cacheName/$environmentId/$serviceKey"
  }
}
