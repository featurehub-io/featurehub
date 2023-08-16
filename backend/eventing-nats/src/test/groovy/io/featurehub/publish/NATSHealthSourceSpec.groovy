package io.featurehub.publish

import cd.connect.app.config.ThreadLocalConfigurationSource
import io.nats.client.Connection
import spock.lang.Specification

class NATSHealthSourceSpec extends Specification {
  NATSSource nats
  Connection conn
  NATSHealthSource source

  def setup() {
    nats = Mock()
    conn = Mock()
    nats.getConnection() >> conn
    source = new NATSHealthSource(nats)
  }

  def cleanup() {
    ThreadLocalConfigurationSource.clearContext()
  }

  def "various conditions for nats health check"() {
    setup:
      conn.getStatus() >> status
      if (disabled)
        ThreadLocalConfigurationSource.createContext(["nats.healthcheck.disabled": "true"])
      source = new NATSHealthSource(nats)
    when:
      def result = source.healthy
    then:
      result == found
    where:
      status                      | disabled || found
      Connection.Status.CLOSED    | true      | true
      Connection.Status.CONNECTED | true      | true
      Connection.Status.CLOSED    | false      | false
      Connection.Status.CONNECTED | true      | true

  }

  def "if nats health check is disabled, it will be ok if NATS is up"() {

  }
}
