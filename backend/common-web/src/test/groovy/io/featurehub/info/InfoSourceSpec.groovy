package io.featurehub.info

import spock.lang.Specification

class InfoSourceSpec extends Specification {
  def "the application name is found"() {
    when: "i ask for the app name"
      def name = new InfoSource().appName()
    then:
      name == 'Sausage'
  }

  def "the application version is found"() {
    when: "i ask for the app version"
      def name = new InfoSource().appVersion()
    then:
      name == '1.3.7-RC'
  }
}
