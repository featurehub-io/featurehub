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
      } else if (parts.size == 2 && parts[0].length == 32) {
        try {
          // 9ceb81668aa04bb397aa5515b2d4c5ed
          // 9ceb8166-8aa0-4bb3-97aa-5515b2d4c5ed
          val envId = parts[0]
          return KeyParts("default", UUID.fromString(envId.substring(0,8) + "-" +
            envId.substring(8,12) + "-" + envId.substring(12,16) + "-" + envId.substring(16,20) + "-" + envId.substring(20)
          ), parts[1])
        } catch (e: Exception) {
          return null
        }
      } else if (parts.size == 2 && parts[0].length == 36) {
        try {
          return KeyParts("default", UUID.fromString(parts[0]), parts[1])
        } catch (e: Exception) {
          return null
        }
      }

      return null
    }
  }

  override fun equals(other: Any?): Boolean {
    if (other is KeyParts) {
      return other.environmentId.equals(environmentId) && other.serviceKey.equals(serviceKey)
    }

    return false
  }

  override fun hashCode(): Int {
    return String.format("%s/%s", environmentId, serviceKey).hashCode()
  }

  override fun toString(): String {
    return "$environmentId/$serviceKey"
  }
}
