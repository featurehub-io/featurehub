package io.featurehub.publish

import io.nats.client.Connection

interface NATSSource {
  val connection: Connection
}
