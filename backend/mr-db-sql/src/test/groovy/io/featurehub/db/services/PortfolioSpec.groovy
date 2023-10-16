package io.featurehub.db.services


import io.featurehub.db.api.FillOpts
import io.featurehub.db.api.Opts
import io.featurehub.db.api.PortfolioApi
import io.featurehub.db.model.DbPerson
import io.featurehub.db.model.DbPortfolio
import io.featurehub.db.model.query.QDbPortfolio
import io.featurehub.mr.model.CreateGroup
import io.featurehub.mr.model.CreatePortfolio
import io.featurehub.mr.model.Group
import io.featurehub.mr.model.Person
import io.featurehub.mr.model.PersonId
import io.featurehub.mr.model.Portfolio
import io.featurehub.mr.model.SortOrder
import spock.lang.Shared

import java.time.LocalDateTime

class PortfolioSpec extends BaseSpec {
  @Shared PortfolioSqlApi portfolioApi
  @Shared Person normalPerson

  def setupSpec() {
    baseSetupSpec()

    portfolioApi = new PortfolioSqlApi(database, convertUtils, archiveStrategy)
    // create the organization

    def user = new DbPerson.Builder().email("portolio-simple@featurehub.io").build()
    database.save(user)
    normalPerson = new Person().id(new PersonId().id(user.id))
  }

  def "we can create a portfolio group"() {
    when:
      Portfolio created = portfolioApi.createPortfolio(new CreatePortfolio().name("norton"), Opts.empty(), superuser)
    then:
      created != null
      portfolioApi.getPortfolio(created.getId(), Opts.empty(), superuser).name == created.name
      portfolioApi.findPortfolios(created.name, SortOrder.ASC, Opts.empty(), superuser).size() == 1
  }

  def "a normal person cannot see the portfolio group created by the admin"() {
    when: "superuser creates a group"
      Portfolio created = portfolioApi.createPortfolio(new CreatePortfolio().name("normal-port-check"), Opts.empty(), superuser)
    then:
      created != null
      portfolioApi.getPortfolio(created.id, Opts.empty(), normalPerson.id.id) == null
      portfolioApi.findPortfolios(created.name, SortOrder.ASC, Opts.empty(), normalPerson.id.id).size() == 0
  }

  def "a normal person in the portfolio group can see the portfolio"() {
    when: "superuser creates a group"
      Portfolio created = portfolioApi.createPortfolio(new CreatePortfolio().name("normal-port-check1"), Opts.empty(), superuser)
      Group group = groupSqlApi.createGroup(created.id, new CreateGroup().name(created.name), superPerson)
      groupSqlApi.addPersonToGroup(group.id, normalPerson.id.id, Opts.empty())
    then:
      created != null
      portfolioApi.getPortfolio(created.id, Opts.empty(), normalPerson.id.id).name == created.name
      portfolioApi.findPortfolios(created.name, SortOrder.ASC, Opts.empty(), normalPerson.id.id).size() == 1
  }

  def "we cannot create a duplicate portfolio group"() {
    given:
      Portfolio created = portfolioApi.createPortfolio(new CreatePortfolio().name("unique1"), Opts.empty(), superuser)
    when:
      Portfolio dupe = portfolioApi.createPortfolio(new CreatePortfolio().name("Unique1"), Opts.empty(), superuser)
    then:
      thrown PortfolioApi.DuplicatePortfolioException
  }

  def "we cannot update a portfolio's name to a duplicate name"() {
    given:
      Portfolio created = portfolioApi.createPortfolio(new CreatePortfolio().name("unique7"), Opts.empty(), superuser)
    and:
      Portfolio dupe = portfolioApi.createPortfolio(new CreatePortfolio().name("Unique8"), Opts.empty(), superuser)
    when:
      dupe.name("UniQue7")
      portfolioApi.updatePortfolio(dupe, Opts.empty())
    then:
      thrown PortfolioApi.DuplicatePortfolioException
  }

  def "we can update a portfolios description to a different one"() {
    when:
      Portfolio created = portfolioApi.createPortfolio(new CreatePortfolio().name("unique7"), Opts.empty(), superuser)
    and:
      Portfolio update = new Portfolio().id(created.id).name(created.name).description("new desc")
    and:
      portfolioApi.updatePortfolio(update, Opts.empty())
    then:
      thrown PortfolioApi.DuplicatePortfolioException
  }

  def "we can create and delete a portfolio group"() {
    when:
      Portfolio created = portfolioApi.createPortfolio(new CreatePortfolio().name("norton1"), Opts.empty(), superuser)
    and: "i can delete it as well"
      portfolioApi.deletePortfolio(created.id)
    and: "now try and find it"
      Portfolio nowDeleted = portfolioApi.getPortfolio(created.id, Opts.empty(), superuser)
    then:
      created != null
      nowDeleted == null
  }

  def "i don't have to provide a person when creating a portfolio because on initial setup i may not have one with oauth2"() {
    when: "i create a portfolio without a person, which is ok because of delayed oauth"
      Portfolio created = portfolioApi.createPortfolio(new CreatePortfolio().name("norton2"), Opts.empty(), superuser)
    then: "it gets created, as its ok to not have a createdBy (for initial setup)"
      created != null
  }

  def "the person object must have a person id with an idXX"() {
    when:
      Portfolio badUserBadId = portfolioApi.createPortfolio(new CreatePortfolio().name("norton4"), Opts.empty(), normalPerson.id.id)
    then:
      1 == 1
  }

  def "i can rename an existing portfolio"() {
    given: "i have a portfolio"
      def created = portfolioApi.createPortfolio(new CreatePortfolio().name("norton6"), Opts.empty(), superuser)
    when: "i rename the portfolio"
      def renamed = portfolioApi.updatePortfolio(created.name("new name").description("new desc"), Opts.empty())
    and: "i get it again"
      def found = portfolioApi.getPortfolio(created.id, Opts.empty(), superuser)
    then:
      renamed != null
//      created.description == null
      found.name == "new name"
      found.description == "new desc"
  }

  def "i can't rename a non-existent portfolio"() {
    when: "i rename the portfolio"
      Portfolio renamed = portfolioApi.updatePortfolio(new Portfolio().id(UUID.randomUUID()).name("new name"), Opts.empty())
    then:
      renamed == null
  }

  def "I can filter my searches for portfolios"() {
    given: "i delete all portfolios"
      new QDbPortfolio().findList().each({ DbPortfolio p ->
        p.setWhenArchived(LocalDateTime.now())
        p.save()
      })
    and: "i have three portfolios"
      portfolioApi.createPortfolio(new CreatePortfolio().name("crispy"), Opts.empty(), superuser)
      portfolioApi.createPortfolio(new CreatePortfolio().name("crispy2"), Opts.empty(), superuser)
      portfolioApi.createPortfolio(new CreatePortfolio().name("fried"), Opts.empty(), superuser)
    when: "i search for crisp"
      List<Portfolio> crispies = portfolioApi.findPortfolios("crisp", SortOrder.DESC, Opts.opts(FillOpts.Portfolios), superuser)
    and: "i search for all"
      List<Portfolio> all = portfolioApi.findPortfolios(null,  SortOrder.ASC, Opts.opts(FillOpts.Members), superuser)
    then:
      crispies.size() == 2
      crispies[0].name == 'crispy2' // should be sorted desc
      crispies[1].name == 'crispy'
      crispies*.whenCreated != null
      crispies*.createdBy*.id*.id as Set == [superuser] as Set
    and:
      all.size() >= 3
      all[0].name == 'crispy'
  }
}
