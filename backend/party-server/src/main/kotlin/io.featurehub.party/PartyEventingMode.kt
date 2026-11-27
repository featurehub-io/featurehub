package io.featurehub.party

import io.featurehub.events.CloudEventsFeature
import io.featurehub.publish.NATSFeature
import io.featurehub.utils.FallbackPropertyConfig
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URI

/**
 * The party-server no longer contains a NATS server. If no external event stream is configured it routes cloud
 * events in-process, which means it must be the only party-server using its database.
 */
object PartyEventingMode {
  private val log: Logger = LoggerFactory.getLogger(PartyEventingMode::class.java)

  const val LOCALHOST_NATS_MESSAGE =
    "nats.urls points at localhost but this image no longer contains a NATS server; remove nats.urls to use " +
      "in-process events, or point it at your NATS server"

  private val localHosts = setOf("localhost", "127.0.0.1", "::1", "[::1]", "0.0.0.0")

  /**
   * The party-server is the only deployment that allows local eventing. An operator can set
   * cloudevents.local.enabled=false to make "no transport configured" a hard error.
   */
  fun enableLocalEventingByDefault() {
    if (FallbackPropertyConfig.getConfig(CloudEventsFeature.LOCAL_ENABLED_PROPERTY) == null) {
      System.setProperty(CloudEventsFeature.LOCAL_ENABLED_PROPERTY, "true")
    }
  }

  /**
   * Logs which eventing mode is active.
   *
   * @return false if the party-server cannot run in the configured mode
   */
  fun checkAndReport(): Boolean {
    return when (val platform = CloudEventsFeature.eventingPlatform()) {
      is CloudEventsFeature.EventingPlatform.External -> {
        if (NATSFeature.isNatsConfigured()) {
          val urls = natsUrls()
          if (urls.isNotEmpty() && urls.all { isLocalhost(it) } && urls.none { canConnect(it) }) {
            log.error(LOCALHOST_NATS_MESSAGE)
            return false
          }
          log.info("party-server: cloud events via NATS ({}); multiple party-server instances supported",
            urls.joinToString(","))
        } else {
          log.info("party-server: cloud events via {}; multiple party-server instances supported",
            platform.sources.joinToString(",") { it.javaClass.simpleName.removeSuffix("EventingFeatureSource") })
        }
        true
      }
      CloudEventsFeature.EventingPlatform.Local -> {
        log.warn("party-server: no external event streaming configured, using in-process events. This must be the ONLY " +
          "party-server instance using this database.")
        true
      }
      CloudEventsFeature.EventingPlatform.None -> {
        log.error("party-server: no external event streaming configured and {}=false, so features cannot reach " +
          "the cache or Edge. Configure an event stream (e.g. nats.urls) or remove {}.",
          CloudEventsFeature.LOCAL_ENABLED_PROPERTY, CloudEventsFeature.LOCAL_ENABLED_PROPERTY)
        false
      }
    }
  }

  private fun natsUrls(): List<String> =
    (FallbackPropertyConfig.getConfig("nats.urls") ?: "").split(",").map { it.trim() }.filter { it.isNotEmpty() }

  fun isLocalhost(url: String): Boolean {
    val host = hostOf(url) ?: return false
    return localHosts.contains(host.lowercase())
  }

  private fun hostOf(url: String): String? {
    return try {
      URI(if (url.contains("://")) url else "nats://$url").host
    } catch (e: Exception) {
      null
    }
  }

  private fun canConnect(url: String): Boolean {
    return try {
      val uri = URI(if (url.contains("://")) url else "nats://$url")
      Socket().use { it.connect(InetSocketAddress(uri.host, if (uri.port > 0) uri.port else 4222), 2000) }
      true
    } catch (e: Exception) {
      false
    }
  }
}
