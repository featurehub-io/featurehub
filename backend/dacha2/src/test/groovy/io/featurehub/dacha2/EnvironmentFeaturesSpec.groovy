package io.featurehub.dacha2

import io.featurehub.dacha.model.CacheEnvironmentFeature
import io.featurehub.dacha.model.CacheFeature
import io.featurehub.dacha.model.PublishEnvironment
import spock.lang.Specification

import java.util.concurrent.CompletableFuture

class EnvironmentFeaturesSpec extends Specification {
  def "multi-threaded access to EnvironmentFeatures does not trigger a concurrent modification exception"() {
    given: "we have an environment features object with no data"
      def env = new EnvironmentFeatures(new PublishEnvironment().featureValues([]))
    and: "we have 20 threads"
      def futures = (1..20).collect { new FeatureHolderThread(env,
        new CacheEnvironmentFeature().feature(new CacheFeature().version(1).id(UUID.randomUUID()))) }
    when: "we update with 20 threads"
      futures.each { it.start() }
    and: "we wait for them to finish"
      futures.each { it.future.join() }
    then:
      1 == 1
  }


/*
{"@timestamp":"2026-04-09T10:12:36.931+0000","message":"feature value updated, storing new value","priority":"TRACE","path":"io.featurehub.dacha2.Dacha2CacheImpl","thread":"pool-5-thread-2","host":"6f237f1744ca","cuke-req-id":"poll-poll-2-1"}
{"@timestamp":"2026-04-09T10:12:36.931+0000","message":"replacing feature class CacheFeatureValue {\n    id: d2e5bd7a-6c76-442f-853e-ce1dd0cd3045\n    key: FEATURE_TITLE_TO_UPPERCASE\n    locked: false\n    value: false\n    rolloutStrategies: []\n    version: 15\n    retired: false\n    personIdWhoChanged: de706f9b-873a-4ed9-8bfd-a2491783ad96\n} with class CacheFeatureValue {\n    id: d2e5bd7a-6c76-442f-853e-ce1dd0cd3045\n    key: FEATURE_TITLE_TO_UPPERCASE\n    locked: false\n    value: true\n    rolloutStrategies: []\n    version: 16\n    retired: false\n    personIdWhoChanged: de706f9b-873a-4ed9-8bfd-a2491783ad96\n}","priority":"TRACE","path":"io.featurehub.dacha2.EnvironmentFeatures","thread":"pool-5-thread-2","host":"6f237f1744ca","cuke-req-id":"poll-poll-2-1"}
{"@timestamp":"2026-04-09T10:12:36.931+0000","message":"replacing feature class CacheFeature {\n    id: 1c00f8da-e466-451b-882e-126f6d55e14d\n    key: FEATURE_TITLE_TO_UPPERCASE\n    valueType: BOOLEAN\n    filters: null\n    version: 1\n} with class CacheFeature {\n    id: 1c00f8da-e466-451b-882e-126f6d55e14d\n    key: FEATURE_TITLE_TO_UPPERCASE\n    valueType: BOOLEAN\n    filters: null\n    version: 1\n}","priority":"TRACE","path":"io.featurehub.dacha2.EnvironmentFeatures","thread":"pool-5-thread-2","host":"6f237f1744ca","cuke-req-id":"poll-poll-2-1"}
{"@timestamp":"2026-04-09T10:12:36.931+0000","message":"new entry in feature array is class CacheEnvironmentFeature {\n    feature: class CacheFeature {\n        id: 1c00f8da-e466-451b-882e-126f6d55e14d\n        key: FEATURE_TITLE_TO_UPPERCASE\n        valueType: BOOLEAN\n        filters: null\n        version: 1\n    }\n    featureProperties: {appName=XYijYYbrMAFm, portfolio=XYijYYbrMAFm}\n    value: class CacheFeatureValue {\n        id: d2e5bd7a-6c76-442f-853e-ce1dd0cd3045\n        key: FEATURE_TITLE_TO_UPPERCASE\n        locked: false\n        value: true\n        rolloutStrategies: []\n        version: 16\n        retired: false\n        personIdWhoChanged: de706f9b-873a-4ed9-8bfd-a2491783ad96\n    }\n}","priority":"TRACE","path":"io.featurehub.dacha2.EnvironmentFeatures","thread":"pool-5-thread-2","host":"6f237f1744ca","cuke-req-id":"poll-poll-2-1"}
{"@timestamp":"2026-04-09T10:12:36.931+0000","message":"etag is now d9bb1086c9912d9b60de0bc3f23394a2 (from '80ffdae1-5007-4de5-95e9-d279b0c681cd1-21-894ae964-5f17-4555-b4bc-683859049dc01-0000-99b2a036-48db-44dd-b859-3f7fed8ac7321-0000-cf1c13af-016e-4ff5-aa3b-5f980f43bf4b1-21-f1100e42-37ea-40d4-8e0d-4d6885bebdd31-9-1c00f8da-e466-451b-882e-126f6d55e14d1-1-285f9e41-7577-4593-9da0-9b9f59f993061-2')","priority":"TRACE","path":"io.featurehub.dacha2.EnvironmentFeatures","thread":"pool-5-thread-2","host":"6f237f1744ca","cuke-req-id":"poll-poll-2-1"}
{"@timestamp":"2026-04-09T10:12:36.931+0000","message":"publishing enriched stream for env 0170a4a3-9fd9-4732-9bc9-c03656d3c6ba -> [FEATURE_TITLE_TO_UPPERCASE]","priority":"TRACE","path":"io.featurehub.enricher.FeatureEnricherProcessor","thread":"pool-5-thread-2","host":"6f237f1744ca","cuke-req-id":"poll-poll-2-1"}
{"@timestamp":"2026-04-09T10:12:36.932+0000","message":"cloudevent publish: type enriched-feature-v1, subject=io.featurehub.events.enricher, id=0170a4a3-9fd9-4732-9bc9-c03656d3c6ba/1775729556931","priority":"TRACE","path":"io.featurehub.events.CloudEventPublisherRegistry
 */

//  def "MD5 etag test"() {
//    given:
//      def str = '80ffdae1-5007-4de5-95e9-d279b0c681cd1-21-894ae964-5f17-4555-b4bc-683859049dc01-0000-99b2a036-48db-44dd-b859-3f7fed8ac7321-0000-cf1c13af-016e-4ff5-aa3b-5f980f43bf4b1-21-f1100e42-37ea-40d4-8e0d-4d6885bebdd31-9-1c00f8da-e466-451b-882e-126f6d55e14d1-1-285f9e41-7577-4593-9da0-9b9f59f993061-2'
//
//  }
}
