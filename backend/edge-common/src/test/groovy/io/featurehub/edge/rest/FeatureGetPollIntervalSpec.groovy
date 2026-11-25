package io.featurehub.edge.rest

import io.featurehub.edge.KeyParts
import io.featurehub.edge.features.DachaFeatureRequestSubmitter
import io.featurehub.edge.features.FeatureRequestResponse
import io.featurehub.edge.features.FeatureRequestSuccess
import io.featurehub.sse.model.FeatureEnvironmentCollection
import jakarta.ws.rs.container.AsyncResponse
import jakarta.ws.rs.core.Response
import org.glassfish.hk2.api.IterableProvider
import spock.lang.Specification
import spock.lang.Unroll

class FeatureGetPollIntervalSpec extends Specification {
  @Unroll
  def "sdk_version #sdkVersion is #sent the environment's pollInterval"() {
    given: "Dacha has an environment with a poll interval override"
      def envId = UUID.randomUUID()
      def key = new KeyParts("default", envId, "ling*orm")
      def env = new FeatureEnvironmentCollection().id(envId).features([]).pollInterval(30)
      def submitter = Mock(DachaFeatureRequestSubmitter)
      submitter.request(_, _, _) >> [new FeatureRequestResponse(env, FeatureRequestSuccess.SUCCESS, key, "kwong", [:], false)]
      def wrappers = Mock(IterableProvider)
      wrappers.iterator() >> [].iterator()
      def processor = new FeatureGetProcessor(submitter, wrappers)
    and:
      def asyncResponse = Mock(AsyncResponse)
      Response resumed = null
    when:
      processor.processGet(asyncResponse, null, ["${envId}/ling*orm".toString()], null, null, null, sdkVersion)
    then:
      1 * asyncResponse.resume(_ as Response) >> { args -> resumed = args[0]; true }
      resumed.status == 200
      (resumed.entity as List<FeatureEnvironmentCollection>)[0].pollInterval == expected
    where:
      sdkVersion || expected | sent
      null       || null     | "not"
      1          || null     | "not"
      2          || 30       | ""
      3          || 30       | ""
  }
}
