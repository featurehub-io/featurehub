package io.featurehub.edge.features

import io.featurehub.edge.FeatureTransformer
import io.featurehub.edge.KeyParts
import io.featurehub.edge.strategies.ClientContext
import io.featurehub.dacha.model.DachaKeyDetailsResponse
import spock.lang.Specification

import java.util.concurrent.CompletableFuture

class FeatureRequestCollectionSpec extends Specification {
  FeatureTransformer transformer
  ClientContext context
  CompletableFuture<List<FeatureRequestResponse>> future
  List<FeatureRequestResponse> responses
  KeyParts kp1
  KeyParts kp2
//  EtagStructureHolder

  def setup() {
    transformer = Mock(FeatureTransformer)
    context = new ClientContext(false)
    future = new CompletableFuture<List<FeatureRequestResponse>>()
    responses = []
    kp1 = new KeyParts("c1", UUID.randomUUID(), "1")
    kp2 = new KeyParts("c1", UUID.randomUUID(), "1")
  }

  def "if the etag holder is invalid, we will get success/failed mix appropriately"() {
    given: "we have an invalid etag holder"
      def holder = new EtagStructureHolder(Map.of(kp1, "x", kp2, "y"), "0", false)
    and: "we have created our collection"
      def coll = new FeatureRequestCollection(2, transformer, context, future, holder)
    when: "we drop our requests in"
      def r1 = Mock(FeatureRequester)
      r1.key >> kp1
      def r2 = Mock(FeatureRequester)
      r2.key >> kp2
      coll.complete(r1)
      coll.complete(r2)
    and: "we wait for the future"
      def responses = future.get()
    then:
      responses.size() == 2
      responses[0].success == FeatureRequestSuccess.NO_SUCH_KEY_IN_CACHE
      responses[1].success == FeatureRequestSuccess.NO_SUCH_KEY_IN_CACHE
  }

  def "if all requests complete and have the same etags, we will get a NO_CHANGE response"() {
    given: "we have an valid etag holder"
      def holder = new EtagStructureHolder(Map.of(kp1, "x", kp2, "y"), "0", true)
    and: "we have created our collection"
      def coll = new FeatureRequestCollection(2, transformer, context, future, holder)
    when: "we drop our requests in"
      def r1 = Mock(FeatureRequester)
      def details1 = new DachaKeyDetailsResponse().etag("x")
      r1.key >> kp1
      r1.details >> details1
      def r2 = Mock(FeatureRequester)
      def details2 = new DachaKeyDetailsResponse().etag("y")
      r2.key >> kp2
      r2.details >> details2
      coll.complete(r1)
      coll.complete(r2)
    and: "we wait for the future"
      def responses = future.get()
    then:
      responses.size() == 2
      responses[0].success == FeatureRequestSuccess.NO_CHANGE
      responses[1].success == FeatureRequestSuccess.NO_CHANGE
  }

  def "if all requests complete and at least one has a different etag, we will get a SUCCESS response"() {
    given: "we have an valid etag holder"
      def holder = new EtagStructureHolder(Map.of(kp1, "x", kp2, "y"), "0", true)
    and: "we have created our collection"
      def coll = new FeatureRequestCollection(2, transformer, context, future, holder)
    when: "we drop our requests in"
      def r1 = Mock(FeatureRequester)
      def details1 = new DachaKeyDetailsResponse().etag("x").features([])
      r1.key >> kp1
      r1.details >> details1
      def r2 = Mock(FeatureRequester)
      def details2 = new DachaKeyDetailsResponse().etag("z").features([]) // different
      r2.key >> kp2
      r2.details >> details2
      coll.complete(r1)
      coll.complete(r2)
    and: "we wait for the future"
      def responses = future.get()
    then:
      responses.size() == 2
      responses[0].success == FeatureRequestSuccess.SUCCESS
      responses[0].etag == 'x'
      responses[1].success == FeatureRequestSuccess.SUCCESS
      responses[1].etag == 'z'
      2 * transformer.transform([], context, false) >> []
  }

  def "if any requests failed, the ones that are successful will get a success response even if their etags are the same"() {
    given: "we have an valid etag holder"
      def holder = new EtagStructureHolder(Map.of(kp1, "x", kp2, "y"), "0", true)
    and: "we have created our collection"
      def coll = new FeatureRequestCollection(2, transformer, context, future, holder)
    when: "we drop our requests in"
      def r1 = Mock(FeatureRequester)
      def details1 = new DachaKeyDetailsResponse().etag("x").features([])
      r1.key >> kp1
      r1.details >> null // request failed
      def r2 = Mock(FeatureRequester)
      def details2 = new DachaKeyDetailsResponse().etag("z").features([]) // different
      r2.key >> kp2
      r2.details >> details2
      coll.complete(r1)
      coll.complete(r2)
    and: "we wait for the future"
      def responses = future.get()
    then:
      responses.size() == 2
      responses[0].success == FeatureRequestSuccess.NO_SUCH_KEY_IN_CACHE
      responses[0].etag == '0'
      responses[1].success == FeatureRequestSuccess.SUCCESS
      responses[1].etag == 'z'
      1 * transformer.transform([], context, false) >> []
  }
}
