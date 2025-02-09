package io.featurehub.db.services

import io.featurehub.db.api.Opts
import io.featurehub.mr.model.CreateApplication
import io.featurehub.mr.model.CreateApplicationRolloutStrategy
import io.featurehub.mr.model.RolloutStrategyAttribute
import io.featurehub.mr.model.RolloutStrategyAttributeConditional
import io.featurehub.mr.model.RolloutStrategyFieldType
import io.featurehub.mr.model.SortOrder
import io.featurehub.mr.model.UpdateApplicationRolloutStrategy
import org.apache.commons.lang3.RandomStringUtils

class ApplicationStrategySpec extends Base3Spec {
  ApplicationRolloutStrategySqlApi appStrategyApi

  def setup() {
    appStrategyApi = new ApplicationRolloutStrategySqlApi(convertUtils, internalFeatureApi)
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
    when: "i list the strategies for the app"
      def list = appStrategyApi.listStrategies(app1.id, 0, 10, null, false, null, Opts.empty())
    then: "there is only 1"
      list.max == 1
      list.page == 0
      list.items.size() == 1
      list.items[0].strategy.name == "phred22"
  }

  void randomStrategies(String prefix, int count, UUID appId) {
    for(int i = 0; i < count; i ++ ) {
      def create = new CreateApplicationRolloutStrategy()
        .name(prefix + ranName()).disabled(false)
        .percentage(20000)
        .attributes([new RolloutStrategyAttribute().id("abcde")
                       .type(RolloutStrategyFieldType.BOOLEAN)
                       .values([true])
                       .fieldName("jxtq")
                       .conditional(RolloutStrategyAttributeConditional.ENDS_WITH)])
      appStrategyApi.createStrategy(appId, create, superuser, Opts.empty())
    }
  }

  // sure, i am a blink, sue me :-)
  def "pagination works for application strategies"() {
    given: "i have a new application specifically for this test"
      def myApp = applicationSqlApi.createApplication(portfolio.id,
        new CreateApplication().name(ranName()).description(ranName()), superPerson)
    and: "i have 10 with the prefix rose"
      randomStrategies("rose", 10, myApp.id)
    and: "10 more with the prefix jennie"
      randomStrategies("jennie", 10, myApp.id)
    when: "i ask for the 1st 10, they are all jennie"
      def list = appStrategyApi.listStrategies(myApp.id, 0, 10,
        null, false, SortOrder.ASC, Opts.empty())
    then:
      list.max == 20
      list.page == 0
      list.items.findAll { it.strategy.name.startsWith("jennie") }.size() == 10
    when: "i ask for the next 10 they are all rose"
      list = appStrategyApi.listStrategies(myApp.id, 1, 10, null, false, null, Opts.empty())
    then:
      list.max == 20
      list.page == 1
      list.items.findAll { it.strategy.name.startsWith("rose") }.size() == 10
    when: "i ask for those like jennie, i get 10 items with a max of 10"
      list = appStrategyApi.listStrategies(myApp.id, 0, 10, "Jennie", false, null, Opts.empty())
    then:
      list.max == 10
      list.page == 0
      list.items.findAll { it.strategy.name.startsWith("jennie") }.size() == 10
    when: "i ask for the second page of jennie with the jennie filter there are no items"
      list = appStrategyApi.listStrategies(myApp.id, 1, 10, "Jennie", false, null, Opts.empty())
    then:
      list.max == 10
      list.page == 1
      list.items.size() == 0
  }
}
