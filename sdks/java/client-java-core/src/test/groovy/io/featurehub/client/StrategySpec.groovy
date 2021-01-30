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

class StrategySpec extends Specification {
  ClientFeatureRepository repo

  def setup() {
    repo = new ClientFeatureRepository(new Executor() {
      @Override
      void execute(Runnable command) {
        command.run();
      }
    })
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
      def cc = new ClientContextRepository().country(StrategyAttributeCountryName.TURKEY)
    and: "we create a context not matching the strategy"
      def ccNot = new ClientContextRepository().country(StrategyAttributeCountryName.NEW_ZEALAND)
    then: "without the context it is true"
      repo.getFlag("bool1")
    and: "with the good context it is false"
      !repo.getFlag("bool1", cc)
    and: "with the bad context it is true"
      repo.getFlag("bool1", ccNot)
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
          ,new RolloutStrategy().value(10).attributes(
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
        def ccFirst = new ClientContextRepository().attr("age", "27")
        def ccNoMatch = new ClientContextRepository().attr("age", "18")
        def ccSecond = new ClientContextRepository().attr("age", "43")
    then: "without the context it is true"
        repo.getNumber("num1") == 16
        repo.getNumber("num1", ccNoMatch) == 16
        repo.getNumber("num1", ccSecond) == 6
        repo.getNumber("num1", ccFirst) == 10
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
           ,new RolloutStrategy().value("older-than-twenty").attributes(
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
        def ccFirst = new ClientContextRepository().attr("age", "27").platform(StrategyAttributePlatformName.IOS)
        def ccNoMatch = new ClientContextRepository().attr("age", "18").platform(StrategyAttributePlatformName.ANDROID)
        def ccSecond = new ClientContextRepository().attr("age", "43").platform(StrategyAttributePlatformName.MACOS)
        def ccThird = new ClientContextRepository().attr("age", "18").platform(StrategyAttributePlatformName.MACOS)
        def ccEmpty = new ClientContextRepository()
    then: "without the context it is true"
        repo.getString("feat1") == "feature"
        repo.getString("feat1", ccNoMatch) == "feature"
        repo.getString("feat1", ccSecond) == "not-mobile"
        repo.getString("feat1", ccFirst) == "older-than-twenty"
        repo.getString("feat1", ccThird) == "not-mobile"
        repo.getString("feat1", ccEmpty) == "feature"
  }
}
