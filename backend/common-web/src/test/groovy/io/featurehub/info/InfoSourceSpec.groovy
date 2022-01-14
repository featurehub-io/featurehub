package io.featurehub.info

import spock.lang.Specification

class InfoSourceSpec extends Specification {
  def "the application version is found"() {
    when: "i ask for the app version"
      def name = new ApplicationVersionDiscovery().appVersion()
    then:
      name == '1.3.7-RC'
  }
}
