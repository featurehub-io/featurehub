package io.featurehub.android

import io.featurehub.client.FeatureHubConfig
import io.featurehub.client.FeatureStore
import okhttp3.Call
import okhttp3.Request
import spock.lang.Specification

class FeatureHubClientSpec extends Specification {
  Call.Factory client
  Call call;
  FeatureStore repo
  FeatureHubClient fhc

  def "a null sdk url will never trigger a call"() {
    when: "i initialize the client"
      call = Mock(Call)
      def fhc = new FeatureHubClient(null, null, null, client, Mock(FeatureHubConfig))
    and: "check for updates"
      fhc.checkForUpdates()
    then:
      0 * client.newCall(_)
  }

  def "a valid host and url will trigger a call when asked"() {
    given: "i validly initialize the client"
      call = Mock(Call)

      client = Mock {
        1 * newCall({ Request r ->
          r.header('x-featurehub') == 'fred=mary'
        }) >> call
      }


      repo = Mock {
      }
      fhc = new FeatureHubClient("http://localhost", ["1234"], repo, client, Mock(FeatureHubConfig))
    and: "i specify a header"
      fhc.contextChange(["fred":["mary"]])
    when: "i check for updates"
      fhc.checkForUpdates()
    then:
      1 == 1
  }

  // can't test any further because okhttp uses too many final classes
}
