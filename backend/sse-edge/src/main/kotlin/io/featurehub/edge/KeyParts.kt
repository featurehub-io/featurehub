package io.featurehub.edge

class KeyParts(cacheName: String, environmentId: String, serviceKey: String) {
  val cacheName = cacheName
    get() = field

  val environmentId = environmentId
    get() = field

  val serviceKey = serviceKey
    get() = field


  companion object {
    fun fromString(apiUrl: String): KeyParts? {
      val parts: List<String> = apiUrl.split("/")
      if (parts.size == 3) {
        return KeyParts(parts[0], parts[1], parts[2])
      }

      return null;
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
