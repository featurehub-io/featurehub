package io.featurehub.party

import spock.lang.Specification

class PartyEventingModeSpec extends Specification {
  def "localhost nats urls are detected"() {
    expect:
      PartyEventingMode.INSTANCE.isLocalhost(url) == local
    where:
      url                           | local
      'nats://localhost:4222'       | true
      'nats://127.0.0.1:4222'       | true
      'localhost:4222'              | true
      'nats://[::1]:4222'           | true
      'nats://LOCALHOST'            | true
      'nats://nats-ormkorn:4222'    | false
      'nats://kohn-kaen.isaan:4222' | false
      'tls://10.0.0.5:4222'         | false
  }
}
