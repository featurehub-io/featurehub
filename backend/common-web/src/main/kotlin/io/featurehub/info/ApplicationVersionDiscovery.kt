package io.featurehub.info

import io.featurehub.rest.InfoResource
import jakarta.inject.Singleton
import jakarta.ws.rs.core.Feature
import jakarta.ws.rs.core.FeatureContext
import org.glassfish.jersey.internal.inject.AbstractBinder
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import java.util.jar.Manifest

interface ApplicationVersion {
  fun appVersion(): String
}

class ApplicationVersionFeatures : Feature {
  override fun configure(context: FeatureContext): Boolean {
    context.register(InfoResource::class.java)
    context.register(object : AbstractBinder() {
      override fun configure() {
        bind(ApplicationVersionDiscovery::class.java).to(ApplicationVersion::class.java).`in`(Singleton::class.java)
      }
    })

    return true
  }
}

private const val VERSION_STRING = "FeatureHub-Version"

class ApplicationVersionDiscovery : ApplicationVersion {
  companion object {
    val log: Logger = LoggerFactory.getLogger(ApplicationVersionDiscovery::class.java)
    val cachedInfo = ConcurrentHashMap<String, String>()
  }

  private fun getInfoVal(key: String): String? {
    var info = cachedInfo.get(key)

    if (info == null) {
      val resources = this.javaClass.classLoader.getResources("META-INF/MANIFEST.MF")

      while (resources.hasMoreElements()) {
        try {
          val nextManifest = resources.nextElement()
          log.trace("processing manifest {}", nextManifest)
          val manifest = Manifest(nextManifest.openStream())
          val attr = manifest.mainAttributes.getValue(key)
          log.trace("attr  is {} - main is {}", attr, manifest.mainAttributes)
          if (attr != null) {
            info = attr.toString()
            cachedInfo[key] = info
            break;
          }
        } catch (ignored: IOException) {
          log.trace("Unable to process manifest", ignored)
        }
      }
    }

    log.trace("Not found, checking for file version")
    // this will try and walk up the file  path assuming you are doing local development
    // - it should only happen when we are developing locally
    if (info == null && key == VERSION_STRING) {
      var currentFolder = File(".")
      val nameIdx = currentFolder.absolutePath.indexOf("/featurehub/")
      if (nameIdx != -1) {
        val file = File(currentFolder.absolutePath.substring(0, nameIdx) + "/featurehub/current-rc.txt")

        log.info("Trying to find current RC version: {}", file.absolutePath)

        if (file.exists()) {
          val lines = file.readLines()
          if (lines.isNotEmpty()) {
            info = lines[0].trim()
            cachedInfo[key] = info
          }
        }

        if (info == null) {
          log.info("No version in {}", file.absolutePath)
        }
      }
    }

    return info
  }

  override fun appVersion(): String {
    return getInfoVal(VERSION_STRING) ?: "<UnknownVersion>"
  }
}

