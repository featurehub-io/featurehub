package io.featurehub.client

import io.featurehub.sse.model.FeatureState
import io.featurehub.sse.model.FeatureValueType
import io.featurehub.sse.model.RolloutStrategy
import io.featurehub.sse.model.RolloutStrategyAttribute
import io.featurehub.sse.model.RolloutStrategyAttributeConditional
import io.featurehub.sse.model.RolloutStrategyFieldType
import io.featurehub.sse.model.StrategyAttributeCountryName
import io.featurehub.sse.model.StrategyAttributePlatformName
import io.featurehub.sse.model.StrategyAttributeWellKnownNames
import spock.lang.Specification

import java.util.concurrent.Executor
import java.util.concurrent.ExecutorService

class StrategySpec extends Specification {
  ClientFeatureRepository repo

  def setup() {
    def exec = [
      execute: { Runnable cmd -> cmd.run() },
      shutdownNow: { -> }
    ] as ExecutorService
    repo = new ClientFeatureRepository(exec)
  }

  def "basic boolean strategy"() {
    given: "i have a basic boolean feature with a rollout strategy"
        def f = new FeatureState()
          .key("bool1")
          .value(true)
          .version(1)
          .type(FeatureValueType.BOOLEAN)
          .strategies([new RolloutStrategy().value(false).attributes(
            [new RolloutStrategyAttribute().type(RolloutStrategyFieldType.STRING)
               .conditional(RolloutStrategyAttributeConditional.EQUALS)
               .fieldName(StrategyAttributeWellKnownNames.COUNTRY.getValue())
               .values([StrategyAttributeCountryName.TURKEY.getValue()])
            ]
          )])
    and: "we have a feature repository with this in it"
        repo.notify([f])
    when: "we create a client context matching the strategy"
        def cc = new TestContext(repo).country(StrategyAttributeCountryName.TURKEY)
    and: "we create a context not matching the strategy"
        def ccNot = new TestContext(repo).country(StrategyAttributeCountryName.NEW_ZEALAND)
    then: "without the context it is true"
        repo.getFeatureState("bool1").boolean
    and: "with the good context it is false"
        !cc.feature("bool1").boolean
        !cc.isEnabled("bool1")
    and: "with the bad context it is true"
        ccNot.feature("bool1").boolean
  }

  def "number strategy"() {
    given: "i have a basic number feature with a rollout strategy"
        def f = new FeatureState()
          .key("num1")
          .value(16)
          .version(1)
          .type(FeatureValueType.NUMBER)
          .strategies([new RolloutStrategy().value(6).attributes(
            [new RolloutStrategyAttribute().type(RolloutStrategyFieldType.NUMBER)
               .conditional(RolloutStrategyAttributeConditional.GREATER_EQUALS)
               .fieldName("age")
               .values([40])
            ]
          )
                       , new RolloutStrategy().value(10).attributes(
            [new RolloutStrategyAttribute().type(RolloutStrategyFieldType.NUMBER)
               .conditional(RolloutStrategyAttributeConditional.GREATER_EQUALS)
               .fieldName("age")
               .values([20])
            ]
          )
          ])
    and: "we have a feature repository with this in it"
        repo.notify([f])
    when: "we create a client context matching the strategy"
        def ccFirst = new TestContext(repo).attr("age", "27")
        def ccNoMatch = new TestContext(repo).attr("age", "18")
        def ccSecond = new TestContext(repo).attr("age", "43")
    then: "without the context it is true"
        repo.getFeatureState("num1").number == 16
        ccNoMatch.feature("num1").number == 16
        ccSecond.feature("num1").number == 6
        ccFirst.feature("num1").number == 10
  }

  def "string strategy"() {
    given: "i have a basic string feature with a rollout strategy"
        def f = new FeatureState()
          .key("feat1")
          .value("feature")
          .version(1)
          .type(FeatureValueType.STRING)
          .strategies([new RolloutStrategy().value("not-mobile").attributes(
            [new RolloutStrategyAttribute().type(RolloutStrategyFieldType.STRING)
               .conditional(RolloutStrategyAttributeConditional.EXCLUDES)
               .fieldName(StrategyAttributeWellKnownNames.PLATFORM.getValue())
               .values([StrategyAttributePlatformName.ANDROID.value, StrategyAttributePlatformName.IOS.value])
            ]
          )
                       , new RolloutStrategy().value("older-than-twenty").attributes(
            [new RolloutStrategyAttribute().type(RolloutStrategyFieldType.NUMBER)
               .conditional(RolloutStrategyAttributeConditional.GREATER_EQUALS)
               .fieldName("age")
               .values([20])
            ]
          )
          ])
    and: "we have a feature repository with this in it"
        repo.notify([f])
    when: "we create a client context matching the strategy"
        def ccFirst = new TestContext(repo).attr("age", "27").platform(StrategyAttributePlatformName.IOS)
        def ccNoMatch = new TestContext(repo).attr("age", "18").platform(StrategyAttributePlatformName.ANDROID)
        def ccSecond = new TestContext(repo).attr("age", "43").platform(StrategyAttributePlatformName.MACOS)
        def ccThird = new TestContext(repo).attr("age", "18").platform(StrategyAttributePlatformName.MACOS)
        def ccEmpty = new TestContext(repo)
    then: "without the context it is true"
        repo.getFeatureState("feat1").string == "feature"
        ccNoMatch.feature("feat1").string == "feature"
        ccSecond.feature("feat1").string == "not-mobile"
        ccFirst.feature("feat1").string == "older-than-twenty"
        ccThird.feature("feat1").string == "not-mobile"
        ccEmpty.feature("feat1").string == "feature"
  }

  def "json strategy"() {
    given: "i have a basic json feature with a rollout strategy"
        def f = new FeatureState()
          .key("feat1")
          .value("feature")
          .version(1)
          .type(FeatureValueType.JSON)
          .strategies([new RolloutStrategy().value("not-mobile").attributes(
            [new RolloutStrategyAttribute().type(RolloutStrategyFieldType.STRING)
               .conditional(RolloutStrategyAttributeConditional.EXCLUDES)
               .fieldName(StrategyAttributeWellKnownNames.PLATFORM.getValue())
               .values([StrategyAttributePlatformName.ANDROID.value, StrategyAttributePlatformName.IOS.value])
            ]
          ), new RolloutStrategy().value("older-than-twenty").attributes(
            [new RolloutStrategyAttribute().type(RolloutStrategyFieldType.NUMBER)
               .conditional(RolloutStrategyAttributeConditional.GREATER_EQUALS)
               .fieldName("age")
               .values([20])
            ]
          )
          ])
    and: "we have a feature repository with this in it"
        repo.notify([f])
    when: "we create a client context matching the strategy"
        def ccFirst = new TestContext(repo).attr("age", "27").platform(StrategyAttributePlatformName.IOS)
        def ccNoMatch = new TestContext(repo).attr("age", "18").platform(StrategyAttributePlatformName.ANDROID)
        def ccSecond = new TestContext(repo).attr("age", "43").platform(StrategyAttributePlatformName.MACOS)
        def ccThird = new TestContext(repo).attr("age", "18").platform(StrategyAttributePlatformName.MACOS)
        def ccEmpty = new TestContext(repo)
    then: "without the context it is true"
        repo.getFeatureState("feat1").rawJson == "feature"
        repo.getFeatureState("feat1").string == null
        ccNoMatch.feature("feat1").rawJson == "feature"
        ccNoMatch.feature("feat1").string == null
        ccSecond.feature("feat1").rawJson == "not-mobile"
        ccFirst.feature("feat1").rawJson == "older-than-twenty"
        ccThird.feature("feat1").rawJson == "not-mobile"
        ccEmpty.feature("feat1").rawJson == "feature"
  }
}
