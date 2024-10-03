package io.featurehub.db.services

import io.featurehub.db.api.Opts
import io.featurehub.mr.model.CreateApplicationRolloutStrategy
import io.featurehub.mr.model.RolloutStrategyAttribute
import io.featurehub.mr.model.RolloutStrategyAttributeConditional
import io.featurehub.mr.model.RolloutStrategyFieldType
import io.featurehub.mr.model.UpdateApplicationRolloutStrategy

class ApplicationStrategySpec extends Base3Spec {
  ApplicationRolloutStrategySqlApi appStrategyApi

  def setup() {
    appStrategyApi = new ApplicationRolloutStrategySqlApi(convertUtils, cacheSource)
  }

  def "i can create, update and delete an application strategy"() {
    given: "i have an app strategy"
      def create = new CreateApplicationRolloutStrategy()
          .name("phred").disabled(false)
          .percentage(20000)
          .attributes([new RolloutStrategyAttribute().id("abcde")
                         .type(RolloutStrategyFieldType.BOOLEAN)
                         .values([true])
                        .fieldName("jxtq")
                         .conditional(RolloutStrategyAttributeConditional.ENDS_WITH)])
    when: "i save it"
      def strat = appStrategyApi.createStrategy(app1.id, create, superuser, Opts.empty())
    then:
      strat.name == "phred"
      strat.percentage == 20000
    when: "i update it with the same name, its ok"
      def updated = appStrategyApi.updateStrategy(app1.id, strat.id,
        new UpdateApplicationRolloutStrategy().name("phred22")
          .attributes([new RolloutStrategyAttribute().id("abcde")
                         .type(RolloutStrategyFieldType.BOOLEAN)
                         .values([false])
                         .fieldName("jxtq")
                         .conditional(RolloutStrategyAttributeConditional.ENDS_WITH)])
          .percentage(10000), superuser, Opts.empty())
    then:
      updated.name == "phred22"
      updated.percentage == 10000
  }
}
