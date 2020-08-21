package io.featurehub.client

import io.featurehub.sse.model.FeatureState
import io.featurehub.sse.model.FeatureValueType
import spock.lang.Specification

import java.util.concurrent.Executor

class StaticFeatureContextSpec extends Specification {

  enum TestFeatures implements Feature {
    banana, peach, peach_quantity, peach_config, not_set
  }

  FeatureRepository repo
  Executor executor

  def setup() {
    executor = {it.run()  } as Executor
    repo = new ClientFeatureRepository(executor)
  }

  def "the static context works as expected"() {
    given: "we have features in the repo"
      def features = [
        new FeatureState().id('1').key(TestFeatures.banana.name()).version(1L).value(true).type(FeatureValueType.BOOLEAN),
        new FeatureState().id('2').key(TestFeatures.peach.name()).version(1L).value("orange").type(FeatureValueType.STRING),
        new FeatureState().id('3').key(TestFeatures.peach_quantity.name()).version(1L).value(17).type(FeatureValueType.NUMBER),
        new FeatureState().id('4').key(TestFeatures.peach_config.name()).version(1L).value("{}").type(FeatureValueType.JSON),

      ]
    and: "i have a feature listener"
      def bananaListener = Mock(FeatureListener)
      StaticFeatureContext.repository = repo
      StaticFeatureContext.instance.addListener(TestFeatures.banana, bananaListener)
    and: "i register an analytics collector"
      def ac = Mock(AnalyticsCollector)
      repo.addAnalyticCollector(ac)
    when: "i register them"
      repo.notify(features)
    and: "log an event"
      StaticFeatureContext.instance.logAnalyticsEvent("action")
    then:
      StaticFeatureContext.instance.isActive(TestFeatures.banana)
      StaticFeatureContext.instance.isSet(TestFeatures.peach_config)
      StaticFeatureContext.instance.isSet(TestFeatures.banana)
      StaticFeatureContext.instance.isSet(TestFeatures.peach_quantity)
      StaticFeatureContext.instance.isSet(TestFeatures.peach)
      StaticFeatureContext.instance.exists(TestFeatures.peach)
      !StaticFeatureContext.instance.isSet(TestFeatures.not_set)
      1 * bananaListener.notify(_)
      1 * ac.logEvent("action", null, _)
  }
}
