package io.featurehub.publish

import cd.connect.app.config.ConfigKey
import cd.connect.app.config.DeclaredConfigResolver
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.IOException

class NATSConnectionSource : NATSSource {
  private val log: Logger = LoggerFactory.getLogger(NATSConnectionSource::class.java)

  @ConfigKey("nats.urls")
  var natsServer = "nats://localhost:4222"
  val natsConnection: io.nats.client.Connection

  init {
    DeclaredConfigResolver.resolve(this)

    val options = io.nats.client.Options.Builder().server(natsServer).build()
    natsConnection = try {
      val conn = io.nats.client.Nats.connect(options)
      log.info("NATS connection successfully established")
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
}
