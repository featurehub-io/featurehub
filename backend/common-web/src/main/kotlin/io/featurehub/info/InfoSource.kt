package io.featurehub.info

import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import java.util.jar.Manifest

class InfoSource {
  companion object {
    val cachedInfo = ConcurrentHashMap<String, String>()
  }

  private fun getInfoVal(key: String): String? {
    var info = cachedInfo.get(key)

    if (info == null) {
      val resources = this.javaClass.classLoader.getResources("META-INF/MANIFEST.MF")

      while (resources.hasMoreElements()) {
        try {
          val manifest = Manifest(resources.nextElement().openStream());
          val attr = manifest.mainAttributes.getValue(key)
          if (attr != null) {
            info = attr.toString()
            cachedInfo[key] = info
          }
        } catch (ignored: IOException) {
        }
      }
    }

    return info
  }

  fun appVersion() : String {
    return getInfoVal("FeatureHub-Version") ?: "<UnknownVersion>"
  }

  fun appName() : String {
    return getInfoVal("FeatureHub-AppName") ?: "<UnknownApp>"
  }
}
