package io.featurehub.client

import io.featurehub.sse.model.FeatureState
import io.featurehub.sse.model.FeatureValueType
import io.featurehub.sse.model.RolloutStrategy
import io.featurehub.sse.model.RolloutStrategyAttribute
import io.featurehub.sse.model.RolloutStrategyAttributeConditional
import io.featurehub.sse.model.RolloutStrategyFieldType
import io.featurehub.sse.model.StrategyAttributeCountryName
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
}
