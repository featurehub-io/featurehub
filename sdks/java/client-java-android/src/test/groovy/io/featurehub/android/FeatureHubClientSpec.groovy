package io.featurehub.android

import okhttp3.Call
import spock.lang.Specification

class FeatureHubClientSpec extends Specification {
  def "a null sdk url will never trigger a call"() {
    when: "i initialize the client"
      def client = Mock(Call.Factory)
      def fhc = new FeatureHubClient(null, null, null, client)
    and: "check for updates"
      fhc.checkForUpdates()
    then:
      0 * client.newCall(_)
  }
}
