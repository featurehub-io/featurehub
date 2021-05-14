package io.featurehub.publish

import io.featurehub.health.HealthSource
import io.nats.client.Connection
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NATSHealthSource
  @Inject
  constructor(val natsSource: NATSSource) : HealthSource {

  override val healthy: Boolean
    get() {
      val status = natsSource.connection.status;
      return status == Connection.Status.CONNECTED || status == Connection.Status.CONNECTING || status == Connection.Status.RECONNECTING
    }

  override val sourceName: String
    get() = "NATS Connection"
}
