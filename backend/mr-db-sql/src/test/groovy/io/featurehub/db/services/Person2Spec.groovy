package io.featurehub.db.services

import io.featurehub.db.api.FillOpts
import io.featurehub.db.api.Opts
import io.featurehub.mr.model.Group
import io.featurehub.mr.model.PersonType
import io.featurehub.mr.model.Portfolio

class Person2Spec extends Base2Spec {
  PersonSqlApi personSqlApi
  PortfolioSqlApi portfolioSqlApi

  def setup() {
    personSqlApi = new PersonSqlApi(db, convertUtils, archiveStrategy, groupSqlApi)
    portfolioSqlApi = new PortfolioSqlApi(db, convertUtils, archiveStrategy)
  }

  def "when i create a new superuser they have access to portfolios and if i remove them, they don't"() {
    given: "i create a new user"
      def person = personSqlApi.createPerson("millie@i.com", "Millie", "password123", superPerson.id.id, Opts.empty())
    and: "I have two new portfolios"
      def port1 = portfolioSqlApi.createPortfolio(new Portfolio().name("port1").description("port1"), Opts.empty(), superPerson)
      def groupPort1 = groupSqlApi.createPortfolioGroup(
        port1.id,
        new Group().name('port1').admin(true),
        superPerson
      )

      def port2 = portfolioSqlApi.createPortfolio(new Portfolio().name("port2").description("port2"), Opts.empty(), superPerson)
      def groupPort2 = groupSqlApi.createPortfolioGroup(
        port2.id,
        new Group().name('port2').admin(true),
        superPerson
      )
    and: "i know what the superuser group is"
      def superuserGroup = groupSqlApi.getSuperuserGroup(convertUtils.organizationId, superPerson)
    when: "i update the person to be a superuser"
      person.addGroupsItem(new Group().id(superuserGroup.id))
      def withSuperuserGroup = personSqlApi.update(person.id.id, person, Opts.opts(FillOpts.Groups), superPerson.id.id)
    and: "then I update them to remove teh superuser group"
      def removedSuperuserGroupOk = withSuperuserGroup.groups.removeIf({ g -> g.id == superuserGroup.id})
      def withoutSuperuserGroup = personSqlApi.update(withSuperuserGroup.id.id, withSuperuserGroup, Opts.opts(FillOpts.Groups), superPerson.id.id)
    then: "it should include the superuser group and the two portfolio groups"
      removedSuperuserGroupOk
      withSuperuserGroup.groups.find({it.id == groupPort1.id})
      withSuperuserGroup.groups.find({it.id == groupPort2.id})
    and: "after removal it shouldn't include the superuser group or the two portfolio groups"
      !withoutSuperuserGroup.groups.find({it.id == superuserGroup.id})
      !withoutSuperuserGroup.groups.find({it.id == groupPort1.id})
      !withoutSuperuserGroup.groups.find({it.id == groupPort2.id})
  }

}
