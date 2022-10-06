package io.featurehub.edge

import cd.connect.app.config.ConfigKey
import cd.connect.app.config.DeclaredConfigResolver
import cd.connect.lifecycle.ApplicationLifecycleManager
import cd.connect.lifecycle.LifecycleStatus
import cd.connect.lifecycle.LifecycleTransition
import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.google.common.cache.RemovalListener
import io.featurehub.dacha.model.PublishFeatureValues
import io.featurehub.edge.client.ClientConnection
import io.featurehub.edge.features.DachaFeatureRequestSubmitter
import io.opentelemetry.context.Context
import io.prometheus.client.Gauge
import jakarta.inject.Inject
import org.slf4j.LoggerFactory
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

interface StreamingFeatureController {
  fun clientRemoved(client: ClientConnection)
  fun requestFeatures(client: ClientConnection)
  fun updateFeatures(features: PublishFeatureValues)
}

data class ConnectionHolder(val client: ClientConnection, val ejectHandler: UUID)

class StreamingFeatureSource @Inject constructor(
  private val dachaFeatureRequestSubmitter: DachaFeatureRequestSubmitter,

  ) : StreamingFeatureController {
  private val updateExecutor: ExecutorService
  private val listenExecutor: ExecutorService

  @ConfigKey("update.pool-size")
  var updatePoolSize: Int? = 10

  @ConfigKey("listen.pool-size")
  var listenPoolSize: Int? = 10

  @ConfigKey("edge.cache.streamed-maximum-environments")
  var maximumEnvironments: Long? = 5000

  init {
    DeclaredConfigResolver.resolve(this)
  }

  private val notifyOnIncomingFeatureUpdate =
    CacheBuilder.newBuilder()
      .maximumSize(maximumEnvironments!!)
      .removalListener(RemovalListener<UUID, MutableCollection<ConnectionHolder>> { notification ->
        if (notification.value!!.isNotEmpty()) {
          log.error("Evicted with active connections")
          notification.value!!.forEach {
            // don't tell us
            it.client.deregisterEjection(it.ejectHandler)
            it.client.close()
          }
        }
        environmentListenerGauge.dec()
      })
      .build(object : CacheLoader<UUID, MutableCollection<ConnectionHolder>>() {
        override fun load(key: UUID): MutableCollection<ConnectionHolder> {
          environmentListenerGauge.inc()
          return ConcurrentLinkedQueue()
        }

      })

  // dispatcher subject based on named-cache, NamedCacheListener
  private val environmentListenerGauge = Gauge.build(
    "edge_envs_sse_listeners", "Unique " +
      "environments with active sse listeners"
  ).register()

  private val sseConnectionListenerGauge = Gauge.build(
    "edge_sse_listeners", "Unique SSE clients"
  ).register()

  init {
    DeclaredConfigResolver.resolve(this)
    updateExecutor = Context.taskWrapping(Executors.newFixedThreadPool(updatePoolSize!!))
    listenExecutor = Context.taskWrapping(Executors.newFixedThreadPool(listenPoolSize!!))

    log.info("connected to stream with cache pool size of `{}", updatePoolSize)

    ApplicationLifecycleManager.registerListener { trans: LifecycleTransition ->
      if (trans.next == LifecycleStatus.TERMINATING) {
        shutdown()
      }
    }
  }

  // unsubscribe all and any listeners
  private fun shutdown() {
    notifyOnIncomingFeatureUpdate.invalidateAll()
  }

  /**
   * this tells the clients that there are messages for them
   */
  /**
   * this tells the clients that there are messages for them
   */
  override fun updateFeatures(features: PublishFeatureValues) {
    if (features.features.isEmpty()) {
      return
    }

    log.trace("sending feature {}", features)

    val environmentId = features.features[0].environmentId

    notifyOnIncomingFeatureUpdate[environmentId].forEach { conn ->
      updateExecutor.submit { conn.client.notifyFeature(features.features) }
    }
  }

  // keep track of expired one so we can walk through periodically and delete them
  //  private Map<String, InflightSdkUrlRequest> expired
  override fun requestFeatures(client: ClientConnection) {
    var clientConnections = notifyOnIncomingFeatureUpdate[client.environmentId]
//
    val key = client.key
    listenExecutor.submit {
      try {
        // this squashes all of the requests and optimises the daylights out of this call so we don't need to
        val request = dachaFeatureRequestSubmitter.request(
          listOf(key),
          client.clientContext, client.etags()
        )

        // ok now hold onto it and as updates come in, we can stream them
        // the order it happens here isn't too critical as we detect version changes on the client
        // so if the client gets an old feature it will reject it
        clientConnections.add(ConnectionHolder(client,
          client.registerEjection { client -> clientRemoved(client) }))
        sseConnectionListenerGauge.inc()
        try {
          client.initResponse(request[0])
        } catch (e: Exception) {
          client.failed("unable to communicate with named cache.")
          clientRemoved(client)
        }
      } catch (nfe: Exception) {
        client.failed("unable to communicate with named cache.")
        clientRemoved(client)
      }
    }
  }

  // responsible for removing a client connection once it has been closed
  // from the list of clients we are notifying about feature changes
  override fun clientRemoved(client: ClientConnection) {
    listenExecutor.submit {
      notifyOnIncomingFeatureUpdate.getIfPresent(client.environmentId)?.let { conns ->
        if (conns.removeIf { it.client == client }) {
          sseConnectionListenerGauge.dec()
        }
      }
    }
  }

  companion object {
    private val log = LoggerFactory.getLogger(StreamingFeatureSource::class.java)
  }
}
