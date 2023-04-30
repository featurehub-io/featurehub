package io.featurehub.publish

import io.nats.client.*
import io.nats.client.support.Status
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.lang.Exception

class NATSErrorListener : ErrorListener {
  private val log: Logger = LoggerFactory.getLogger(NATSErrorListener::class.java)

  override fun errorOccurred(conn: Connection, error: String) {
    log.error("NATS error {}", error)
  }

  override fun exceptionOccurred(conn: Connection?, exp: Exception?) {
    log.error("NATS failure", exp)
  }

  override fun messageDiscarded(conn: Connection?, msg: Message?) {
    log.error("NATS message discarded")
  }

  override fun heartbeatAlarm(
    conn: Connection?,
    sub: JetStreamSubscription?,
    lastStreamSequence: Long,
    lastConsumerSequence: Long
  ) {
    log.error("NATS heartbeat alarm")
  }

  override fun unhandledStatus(conn: Connection?, sub: JetStreamSubscription?, status: Status?) {
    log.error("unhanded status {}", status)
  }
}
