package io.featurehub.client

import io.featurehub.client.interceptor.SystemPropertyValueInterceptor
import io.featurehub.sse.model.FeatureState
import io.featurehub.sse.model.FeatureValueType
import spock.lang.Specification

class InterceptorSpec extends Specification {
  def "a system property interceptor returns the correct overridden value"() {
    given: "we have a repository"
      def fr = new ClientFeatureRepository(1);
    and: "we set the system property value interceptor on it"
      fr.registerValueInterceptor(true, new SystemPropertyValueInterceptor())
    when: "we set the feature override"
      def featureName = "feature_one"
      def name = SystemPropertyValueInterceptor.FEATURE_TOGGLES_PREFIX + featureName
      System.setProperty(name, "true")
      System.setProperty(SystemPropertyValueInterceptor.FEATURE_TOGGLES_ALLOW_OVERRIDE, "true")
    then:
      fr.getFeatureState(featureName).boolean
      fr.getFeatureState(featureName).string == 'true'
      fr.getFeatureState(featureName).number == null
      fr.getFeatureState("feature_none").string == null
      !fr.getFeatureState("feature_none").boolean
  }

  def "we can deserialize json in an override"() {
    given: "we have a repository"
      def fr = new ClientFeatureRepository(1);
    and: "we set the system property value interceptor on it"
      fr.registerValueInterceptor(true, new SystemPropertyValueInterceptor())
    when: "we set the feature override"
      def featureName = 'feature_json'
      def name = SystemPropertyValueInterceptor.FEATURE_TOGGLES_PREFIX + "feature_json"
      def rawJson = '{"sample":18}'
      System.setProperty(name, rawJson)
      System.setProperty(SystemPropertyValueInterceptor.FEATURE_TOGGLES_ALLOW_OVERRIDE, "true")
    then:
      !fr.getFeatureState(featureName).boolean
      fr.getFeatureState(featureName).string == rawJson
      fr.getFeatureState(featureName).rawJson == rawJson
      fr.getFeatureState(featureName).getJson(BananaSample) instanceof BananaSample
      fr.getFeatureState(featureName).getJson(BananaSample).sample == 18
      fr.getFeatureState("feature_none").getJson(BananaSample) == null
  }

  def "we can deserialize a number in an override"() {
    given: "we have a repository"
      def fr = new ClientFeatureRepository(1);
    and: "we set the system property value interceptor on it"
      fr.registerValueInterceptor(true, new SystemPropertyValueInterceptor())
    when: "we set the feature override"
      def featureName = 'feature_num'
      def name = SystemPropertyValueInterceptor.FEATURE_TOGGLES_PREFIX + featureName
      def numString = '17.65'
      System.setProperty(name, numString)
      System.setProperty(SystemPropertyValueInterceptor.FEATURE_TOGGLES_ALLOW_OVERRIDE, "true")
    then:
      !fr.getFeatureState(featureName).boolean
      fr.getFeatureState(featureName).string == numString
      fr.getFeatureState(featureName).rawJson == numString
      fr.getFeatureState(featureName).number == 17.65
      fr.getFeatureState('feature_none').number == null
  }

  def "if system property loader is turned off, overrides are ignored"() {
    given: "we have a repository"
      def fr = new ClientFeatureRepository(1);
    and: "we set the system property value interceptor on it"
      fr.registerValueInterceptor(true, new SystemPropertyValueInterceptor())
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

  def "if a feature is locked, we won't call an interceptor that is overridden"() {
    given:
      def fr = new ClientFeatureRepository(1);
      fr.registerValueInterceptor(false, Mock(FeatureValueInterceptor))
    and: "we register a feature"
      fr.notify([new FeatureState().value(true).type(FeatureValueType.BOOLEAN).key("x").id("1").l(true)])
    when: "i ask for the feature"
     def f = fr.getFeatureState("x").boolean
    then:
      f
  }

  def "we can override registered feature values"() {
    given: "we have a repository"
      def fr = new ClientFeatureRepository(1);
    and: "we set the system property value interceptor on it"
      fr.registerValueInterceptor(true, new SystemPropertyValueInterceptor())
    and: "we have a set of features and register them"
      def banana = new FeatureState().id('1').key('banana_or').version(1L).value(false).type(FeatureValueType.BOOLEAN)
      def orange = new FeatureState().id('2').key('peach_or').version(1L).value("orange").type(FeatureValueType.STRING)
      def peachQuantity = new FeatureState().id('3').key('peach-quantity_or').version(1L).value(17).type(FeatureValueType.NUMBER)
      def peachConfig = new FeatureState().id('4').key('peach-config_or').version(1L).value("{}").type(FeatureValueType.JSON)
      def features = [banana, orange, peachConfig, peachQuantity]
      fr.notify(features)
    when: "we set the feature override"
      System.setProperty(SystemPropertyValueInterceptor.FEATURE_TOGGLES_PREFIX + banana.key, "true")
      System.setProperty(SystemPropertyValueInterceptor.FEATURE_TOGGLES_PREFIX + orange.key, "nectarine")
      System.setProperty(SystemPropertyValueInterceptor.FEATURE_TOGGLES_PREFIX + peachQuantity.key, "13")
      System.setProperty(SystemPropertyValueInterceptor.FEATURE_TOGGLES_PREFIX + peachConfig.key, '{"sample":12}')
      System.setProperty(SystemPropertyValueInterceptor.FEATURE_TOGGLES_ALLOW_OVERRIDE, "true")
    then:
      fr.getFeatureState(banana.key).boolean
      fr.getFeatureState(orange.key).string == 'nectarine'
      fr.getFeatureState(peachQuantity.key).number == 13
      fr.getFeatureState(peachConfig.key).rawJson == '{"sample":12}'
      fr.getFeatureState(peachConfig.key).getJson(BananaSample).sample == 12

  }
}
