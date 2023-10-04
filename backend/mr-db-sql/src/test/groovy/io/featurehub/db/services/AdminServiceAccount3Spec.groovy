package io.featurehub.db.services

import io.featurehub.db.api.FillOpts
import io.featurehub.db.api.Opts
import io.featurehub.mr.model.Application
import io.featurehub.mr.model.ApplicationGroupRole
import io.featurehub.mr.model.ApplicationRoleType
import io.featurehub.mr.model.CreateApplication
import io.featurehub.mr.model.CreateGroup
import io.featurehub.mr.model.CreatePortfolio
import io.featurehub.mr.model.Group
import io.featurehub.mr.model.Portfolio
import io.featurehub.mr.model.UpdatePerson

class AdminServiceAccount3Spec extends Base3Spec {
  PersonSqlApi personSqlApi

  def setup() {
    personSqlApi = new PersonSqlApi(db, convertUtils, archiveStrategy, groupSqlApi)
  }

  def "i can update the groups of a service account user"() {
    given: "i have a second setup portfolio"
      def portfolio2 = portfolioSqlApi.createPortfolio(new CreatePortfolio().name(ranName()).description("desc1"), Opts.empty(), superuser)
      def app2 = applicationSqlApi.createApplication(portfolio.id, new CreateApplication().name(ranName()).description("app1"), superPerson)
      def group2 = groupSqlApi.createGroup(portfolio2.id, new CreateGroup().name(ranName()).applicationRoles(
          [new ApplicationGroupRole().roles([ApplicationRoleType.EDIT]).applicationId(app2.id)]
      ), superPerson)
    and: "a group attached to the first one"
      def group1 = groupSqlApi.createGroup(portfolio.id, new CreateGroup().name(ranName()).applicationRoles(
        [new ApplicationGroupRole().roles([ApplicationRoleType.EDIT]).applicationId(app1.id)]
      ), superPerson)
    and: "a service account user"
      def sa1 = personSqlApi.createServicePerson(ranName(), superuser)
    when: "i update the service account with the first group"
      personSqlApi.updateV2(sa1.person.id.id, new UpdatePerson().version(sa1.person.version).groups([group1.id]), superuser)
      def sa2 = personSqlApi.get(sa1.person.id.id, Opts.opts(FillOpts.Groups))
    then:
      sa2.groups.size() == 1
      sa2.groups[0].id == group1.id
    when: "i add in the second group"
      personSqlApi.updateV2(sa1.person.id.id, new UpdatePerson().version(sa2.version).groups([group1.id, group2.id]), superuser)
      def sa3 = personSqlApi.get(sa1.person.id.id, Opts.opts(FillOpts.Groups))
    then:
      sa3.groups.size() == 2
      sa3.groups.find({it.id == group1.id})
      sa3.groups.find({it.id == group2.id})
  }

}
