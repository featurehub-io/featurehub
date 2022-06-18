package io.featurehub.utils

import cd.connect.app.config.ThreadLocalConfigurationSource

class FallbackPropertyConfig {
  companion object {
    fun getConfig(name: String): String? {
      val config = ThreadLocalConfigurationSource.getKey(name) ?: (System.getenv(name) ?: System.getProperty(name))

      if (config == null) { // fallback to the old way of doing things
        val check2 = System.getenv(name.uppercase())
        return check2 ?: System.getenv(name.uppercase().replace(".", "_").replace("-", "_"))
      }

      return config
    }

    fun getMandatoryConfig(name: String): String {
      return getConfig(name) ?: throw RuntimeException("config `${name}` is missing an is required.")
    }

    fun getConfig(name: String, defaultVal: String): String {
      return getConfig(name) ?: defaultVal
    }
  }
}
