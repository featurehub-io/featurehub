package io.featurehub.edge

import java.util.*

class KeyParts(val cacheName: String, val environmentId: UUID, val serviceKey: String) {

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
}
