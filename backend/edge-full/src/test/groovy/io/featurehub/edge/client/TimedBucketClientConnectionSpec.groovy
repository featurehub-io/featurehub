package io.featurehub.edge.client

import io.featurehub.dacha.model.CacheEnvironmentFeature
import io.featurehub.dacha.model.CacheFeature
import io.featurehub.dacha.model.CacheFeatureValue
import io.featurehub.dacha.model.PublishAction
import io.featurehub.dacha.model.PublishFeatureValue
import io.featurehub.edge.FeatureTransformer
import io.featurehub.edge.KeyParts
import io.featurehub.edge.bucket.BucketService
import io.featurehub.edge.features.FeatureRequestResponse
import io.featurehub.edge.features.FeatureRequestSuccess
import io.featurehub.edge.stats.StatRecorder
import io.featurehub.mr.model.FeatureValueType
import io.featurehub.sse.model.FeatureEnvironmentCollection
import io.featurehub.sse.model.FeatureState
import spock.lang.Specification

class TimedBucketClientConnectionSpec extends Specification {
  EventOutputHolder output
  KeyParts apiKey
  FeatureTransformer featureTransformer
  StatRecorder statRecorder
  BucketService bucketService

  TimedBucketClientConnection conn

  def setup() {
    output = Mock()
    featureTransformer = Mock()
    statRecorder = Mock()
    bucketService = Mock()

    apiKey = new KeyParts("cache", UUID.randomUUID(), "key")

    conn = new TimedBucketClientConnection(output, apiKey, featureTransformer,
      statRecorder, null, null, null, bucketService)
  }

  // IGNORE: too much parallelism makes this too expensive a check, do in client
  // if we get create featuer (1), update feature (2), retire feature (3), unretire feature (4)
  // and it turns up as 1 2 4 3, then (3) won't be passed down the wire
//  def "an out of order retiring of a feature is ignored"() {
//    given: "i pass the initial set of feature states as a single feature"
//      def state = new FeatureState().id(UUID.randomUUID()).version(1).type(FeatureValueType.BOOLEAN)
//        .value(Boolean.FALSE).key("x").l(false)
//      def coll = new FeatureEnvironmentCollection().id(UUID.randomUUID())
//          .features([state])
//      def publishedFeature = new PublishFeatureValue()
//        .action(PublishAction.CREATE)
//        .environmentId(UUID.randomUUID())
//        .feature(new CacheEnvironmentFeature()
//          .feature(new CacheFeature().id(state.id).valueType(FeatureValueType.BOOLEAN))
//          .value(new CacheFeatureValue().id(UUID.randomUUID())
//            .retired(false)
//            .value(Boolean.TRUE).version(2).locked(false))
//        )
////    and: "i mock out the feature transformer"
////      featureTransformer.transform(_, _) >> new HashMap()
//    when: "i send the initial response"
//      conn.initResponse(new FeatureRequestResponse(coll, FeatureRequestSuccess.SUCCESS, apiKey, "etag", null))
//    then: "i get some data written out"
//      1 * output.write('features', _, 'etag', _)
//    when: 'i send an update to the feature'
//      conn.notifyFeature([publishedFeature])
//    then: "i get an update"
//      1 * output.write('feature', _, null, _)
//    when: 'i get an out of order 4th version'
//      publishedFeature.feature.value.version = 4
//      publishedFeature.action = PublishAction.UPDATE
//      conn.notifyFeature([publishedFeature])
//    then: "i get an update"
//      1 * output.write('feature', _, null, _)
//    when: 'i get the 3rd version, it is ignored'
//      publishedFeature.feature.value.version = 3
//      publishedFeature.action = PublishAction.DELETE
//      publishedFeature.feature.value.retired(true)
//      conn.notifyFeature([publishedFeature])
//    then: "i do NOT get an update"
//      0 * output.write(_, _, _, _)
//  }
}
