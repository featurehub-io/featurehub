package io.featurehub.events

import spock.lang.Specification

class CloudEventsFeatureSpec extends Specification {
  def cleanup() {
    System.clearProperty(CloudEventsFeature.LOCAL_ENABLED_PROPERTY)
    FakeEventingFeatureSource.enabledFlag = false
  }

  def "with nothing enabled and the local flag set, local eventing is chosen"() {
    given:
      System.setProperty(CloudEventsFeature.LOCAL_ENABLED_PROPERTY, "true")
    expect:
      CloudEventsFeature.@Companion.eventingPlatform() == CloudEventsFeature.EventingPlatform.Local.INSTANCE
      CloudEventsFeature.@Companion.publishingEnabled()
  }

  def "with an external platform and the local flag set, only the external platform is chosen"() {
    given:
      System.setProperty(CloudEventsFeature.LOCAL_ENABLED_PROPERTY, "true")
      FakeEventingFeatureSource.enabledFlag = true
    when:
      def platform = CloudEventsFeature.@Companion.eventingPlatform()
    then:
      platform instanceof CloudEventsFeature.EventingPlatform.External
      (platform as CloudEventsFeature.EventingPlatform.External).sources*.class == [FakeEventingFeatureSource]
      CloudEventsFeature.@Companion.publishingEnabled()
  }

  def "with an external platform and no local flag, the external platform is chosen"() {
    given:
      FakeEventingFeatureSource.enabledFlag = true
    expect:
      CloudEventsFeature.@Companion.eventingPlatform() instanceof CloudEventsFeature.EventingPlatform.External
      CloudEventsFeature.@Companion.publishingEnabled()
  }

  def "with nothing enabled and no local flag, there is no eventing platform"() {
    expect:
      CloudEventsFeature.@Companion.eventingPlatform() == CloudEventsFeature.EventingPlatform.None.INSTANCE
      !CloudEventsFeature.@Companion.publishingEnabled()
  }

  def "the local flag must be explicitly true"() {
    given:
      System.setProperty(CloudEventsFeature.LOCAL_ENABLED_PROPERTY, value)
    expect:
      CloudEventsFeature.@Companion.eventingPlatform() == CloudEventsFeature.EventingPlatform.None.INSTANCE
    where:
      value << ["false", "", "ormkorn"]
  }
}
