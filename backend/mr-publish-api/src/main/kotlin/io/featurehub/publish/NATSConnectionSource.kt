package io.featurehub.publish

import cd.connect.app.config.ConfigKey
import cd.connect.app.config.DeclaredConfigResolver
import java.io.IOException

class NATSConnectionSource : NATSSource {
    @ConfigKey("nats.urls")
  var natsServer = "nats://localhost:4222"
  val natsConnection: io.nats.client.Connection

  init {
      DeclaredConfigResolver.resolve(this)

    val options = io.nats.client.Options.Builder().server(natsServer).build()
    natsConnection = try {
        io.nats.client.Nats.connect(options)
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
