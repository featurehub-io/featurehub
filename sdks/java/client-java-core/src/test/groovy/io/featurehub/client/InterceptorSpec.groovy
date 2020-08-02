package io.featurehub.client

import io.featurehub.client.interceptor.SystemPropertyValueInterceptor
import spock.lang.Specification

class InterceptorSpec extends Specification {
  def "a system property interceptor returns the correct overridden value"() {
    given: "we have a repository"
      def fr = new ClientFeatureRepository(1);
    and: "we set the system property value interceptor on it"
      fr.registerValueInterceptor(new SystemPropertyValueInterceptor())
    when: "we set the feature override"
      def name = SystemPropertyValueInterceptor.FEATURE_TOGGLES_PREFIX + "feature_one"
      System.setProperty(name, "true")
      System.setProperty(SystemPropertyValueInterceptor.FEATURE_TOGGLES_ALLOW_OVERRIDE, "true")
    then:
      fr.getFeatureState("feature_one").boolean
      fr.getFeatureState("feature_one").string == 'true'
      fr.getFeatureState("feature_none").string == null
      !fr.getFeatureState("feature_none").boolean
  }

  def "if system property loader is turned off, overrides are ignored"() {
    given: "we have a repository"
      def fr = new ClientFeatureRepository(1);
    and: "we set the system property value interceptor on it"
      fr.registerValueInterceptor(new SystemPropertyValueInterceptor())
    when: "we set the feature override"
      def name = SystemPropertyValueInterceptor.FEATURE_TOGGLES_PREFIX + "feature_one"
      System.setProperty(name, "true")
      System.clearProperty(SystemPropertyValueInterceptor.FEATURE_TOGGLES_ALLOW_OVERRIDE)
    then:
      !fr.getFeatureState("feature_one").boolean
      fr.getFeatureState("feature_one").string == null
      fr.getFeatureState("feature_none").string == null
      !fr.getFeatureState("feature_none").boolean
  }
}
