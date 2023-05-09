package io.featurehub.publish

import cd.connect.app.config.ConfigKey
import cd.connect.app.config.DeclaredConfigResolver
import io.cloudevents.CloudEvent
import io.featurehub.events.nats.NatsCloudEventQueueListener
import io.featurehub.events.nats.NatsCloudEventTopicListener
import io.featurehub.events.nats.NatsCloudEventsPublisher
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.IOException

class NATSConnectionSource : NATSSource {
  private val log: Logger = LoggerFactory.getLogger(NATSConnectionSource::class.java)

  @ConfigKey("nats.urls")
  var natsServers: List<String> = listOf("nats://localhost:4222")
  val natsConnection: io.nats.client.Connection

  init {
    DeclaredConfigResolver.resolve(this)

    val options = io.nats.client.Options.Builder()
      .errorListener(NATSErrorListener())
      .servers(natsServers.toTypedArray()).build()
    natsConnection = try {
      val conn = io.nats.client.Nats.connect(options)
      log.info("NATS connection successfully established $natsServers")
      conn
    } catch (e: IOException) {
      // should fail if we can't connect
      throw RuntimeException(e)
    } catch (e: InterruptedException) {
      throw RuntimeException(e)
    }

  }

  override val connection: io.nats.client.Connection
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
}
