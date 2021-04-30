package io.featurehub.client

import spock.lang.Specification

import java.util.concurrent.CompletableFuture

class ServerEvalContextSpec extends Specification {
  def config
  def repo
  def edge

  def setup() {
    config = Mock(FeatureHubConfig)
    repo = Mock(FeatureRepositoryContext)
    edge = Mock(EdgeService)
  }

  def "a server eval context should allow a build which should trigger a poll"() {
    given: "i have the requisite setup"
      def scc = new ServerEvalFeatureContext(config, repo, { -> edge})
      edge.isRequiresReplacementOnHeaderChange() >> false
    when: "i attempt to build"
      scc.build();
      scc.userKey("fred").build()
      scc.clear().build();
    then: ""
      2 * edge.contextChange(null) >> {
        def future = new CompletableFuture<>()
        future.complete(scc);
        return future;
      }
      1 * edge.contextChange("userkey=fred") >> {
        def future = new CompletableFuture<>()
        future.complete(scc);
        return future;
      }
  }
}
