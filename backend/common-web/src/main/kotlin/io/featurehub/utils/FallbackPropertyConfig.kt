package io.featurehub.utils

class FallbackPropertyConfig {
  companion object {
    fun getConfig(name: String): String? {
      val config = System.getProperty(name, System.getenv(name))

      if (config == null) {
        val check2 = System.getenv(name.uppercase())
        return check2 ?: System.getenv(name.uppercase().replace(".", "_").replace("-", "_"))
      }

      return config
    }

    fun getConfig(name: String, defaultVal: String): String {
      return getConfig(name) ?: defaultVal
    }
  }
}
