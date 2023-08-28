package io.featurehub.publish

import io.featurehub.health.HealthSource
import io.featurehub.utils.FallbackPropertyConfig
import io.nats.client.Connection
import jakarta.inject.Inject

class NATSHealthSource
  @Inject
  constructor(val natsSource: NATSSource) : HealthSource {
    val natsDisabled: Boolean

  init {
    // check if we want to opt-out of nats being on the healthcheck
    natsDisabled = FallbackPropertyConfig.getConfig("nats.healthcheck.disabled", "false") == "true"
  }

  override val healthy: Boolean
    get() {
      if (natsDisabled) {
        return true
      }

      val status = natsSource.connection.status;
      return status == Connection.Status.CONNECTED || status == Connection.Status.CONNECTING || status == Connection.Status.RECONNECTING
    }

  override val sourceName: String
    get() = "NATS Connection"
}
