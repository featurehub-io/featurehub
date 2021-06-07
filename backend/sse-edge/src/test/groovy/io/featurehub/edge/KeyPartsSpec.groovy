package io.featurehub.edge

import spock.lang.Specification

class KeyPartsSpec extends Specification {
  def "when i have a valid string format i have a valid spec"() {
    when:
      def envId = UUID.randomUUID()
      def x = KeyParts.@Companion.fromString(String.format("a/%s/c", envId))
    then:
      x.cacheName == 'a'
      x.environmentId == envId
      x.serviceKey == 'c'
  }

  def "when the string is invalid, fromString returns null"() {
    when:
      def x = KeyParts.@Companion.fromString("a/b")
    then:
      x == null
  }

  def "i cannot create a key part with null params"() {
    when:
      def x = new KeyParts(null, null, null)
    then:
      thrown(NullPointerException)
  }
}
