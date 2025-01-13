package io.featurehub.publish

import io.cloudevents.CloudEvent
import io.featurehub.events.EventingConnection
import io.featurehub.events.nats.NatsCloudEventQueueListener
import io.featurehub.events.nats.NatsCloudEventTopicListener
import io.featurehub.events.nats.NatsCloudEventsPublisher
import io.featurehub.utils.FallbackPropertyConfig
import io.nats.client.Connection
import io.nats.client.ConnectionListener
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.IOException

class NATSConnectionSource : NATSSource, EventingConnection {
  private val handlers: MutableList<(event: EventingConnection.ConnectionStatus) -> Unit> = mutableListOf()
  private val log: Logger = LoggerFactory.getLogger(NATSConnectionSource::class.java)

  var natsServers: List<String> = FallbackPropertyConfig.getConfig("nats.urls", "nats://localhost:4222")
    .split(",").map { s -> s.trim() }
  private val natsConnection: Connection

  init {
    val options = io.nats.client.Options.Builder()
      .errorListener(NATSErrorListener())
      .connectionListener { conn, e ->
        log.info("NATS connection status {}, discovered servers {}, event {}", conn.status, conn.servers, e)

        if (e == ConnectionListener.Events.CONNECTED || e == ConnectionListener.Events.RECONNECTED ||
          e == ConnectionListener.Events.RESUBSCRIBED
        ) {
          notifyHandlers(EventingConnection.ConnectionStatus.CONNECTED)
        } else {
          notifyHandlers(EventingConnection.ConnectionStatus.DISCONNECTED)
        }
      }
      .servers(natsServers.toTypedArray()).build()

    natsConnection = try {
      val conn = io.nats.client.Nats.connect(options)
      conn
    } catch (e: IOException) {
      // should fail if we can't connect
      throw RuntimeException(e)
    } catch (e: InterruptedException) {
      throw RuntimeException(e)
    }

  }

  private fun notifyHandlers(connected: EventingConnection.ConnectionStatus) {
    handlers.forEach { it(connected) }
  }

  override val connection: Connection
    get() = natsConnection

  override fun createTopicListener(subject: String, handler: (event: CloudEvent) -> Unit): NatsCloudEventTopicListener {
    return NatsCloudEventTopicListener(this, subject, handler)
  }

  override fun createQueueListener(
    subject: String,
    queue: String,
    handler: (event: CloudEvent) -> Unit
  ): NatsCloudEventQueueListener {
    return NatsCloudEventQueueListener(this, subject, queue, handler)
  }

  override fun createPublisher(subject: String): NatsCloudEventsPublisher {
    return NatsCloudEventsPublisher(this, subject)
  }

  override fun registerForConnectionEvents(handler: (event: EventingConnection.ConnectionStatus) -> Unit) {
    this.handlers.add(handler)
  }

  override fun status(): EventingConnection.ConnectionStatus {
    return if (connection.status == Connection.Status.CONNECTED) EventingConnection.ConnectionStatus.CONNECTED else EventingConnection.ConnectionStatus.DISCONNECTED
  }
}
