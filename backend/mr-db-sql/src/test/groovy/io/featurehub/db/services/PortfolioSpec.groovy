package io.featurehub.db.services


import io.featurehub.db.api.FillOpts
import io.featurehub.db.api.Opts
import io.featurehub.db.api.PortfolioApi
import io.featurehub.db.model.DbPerson
import io.featurehub.db.model.DbPortfolio
import io.featurehub.db.model.query.QDbPortfolio
import io.featurehub.mr.model.Group
import io.featurehub.mr.model.Person
import io.featurehub.mr.model.PersonId
import io.featurehub.mr.model.Portfolio
import io.featurehub.mr.model.SortOrder
import spock.lang.Shared

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
      Portfolio created = portfolioApi.createPortfolio(new Portfolio().name("norton").organizationId(org.getId()), Opts.empty(), superPerson)
    then:
      created != null
      portfolioApi.getPortfolio(created.getId(), Opts.empty(), superPerson).name == created.name
      portfolioApi.findPortfolios(created.name, SortOrder.ASC, Opts.empty(), superPerson).size() == 1
  }

  def "a normal person cannot see the portfolio group created by the admin"() {
    when: "superuser creates a group"
      Portfolio created = portfolioApi.createPortfolio(new Portfolio().name("normal-port-check").organizationId(org.getId()), Opts.empty(), superPerson)
    then:
      created != null
      portfolioApi.getPortfolio(created.id, Opts.empty(), normalPerson) == null
      portfolioApi.findPortfolios(created.name, SortOrder.ASC, Opts.empty(), normalPerson).size() == 0
  }

  def "a normal person in the portfolio group can see the portfolio"() {
    when: "superuser creates a group"
      Portfolio created = portfolioApi.createPortfolio(new Portfolio().name("normal-port-check1").organizationId(org.getId()), Opts.empty(), superPerson)
      Group group = groupSqlApi.createGroup(created.id, new Group().name(created.name), superPerson)
      groupSqlApi.addPersonToGroup(group.id, normalPerson.id.id, Opts.empty())
    then:
      created != null
      portfolioApi.getPortfolio(created.id, Opts.empty(), normalPerson).name == created.name
      portfolioApi.findPortfolios(created.name, SortOrder.ASC, Opts.empty(), normalPerson).size() == 1
  }

  def "we cannot create a duplicate portfolio group"() {
    given:
      Portfolio created = portfolioApi.createPortfolio(new Portfolio().name("unique1").organizationId(org.getId()), Opts.empty(), superPerson)
    when:
      Portfolio dupe = portfolioApi.createPortfolio(new Portfolio().name("Unique1").organizationId(org.getId()), Opts.empty(), superPerson)
    then:
      thrown PortfolioApi.DuplicatePortfolioException
  }

  def "we cannot update a portfolio's name to a duplicate name"() {
    given:
      Portfolio created = portfolioApi.createPortfolio(new Portfolio().name("unique7").organizationId(org.getId()), Opts.empty(), superPerson)
    and:
      Portfolio dupe = portfolioApi.createPortfolio(new Portfolio().name("Unique8").organizationId(org.getId()), Opts.empty(), superPerson)
    when:
      dupe.name("UniQue7")
      portfolioApi.updatePortfolio(dupe, Opts.empty())
    then:
      thrown PortfolioApi.DuplicatePortfolioException
  }

  def "we can update a portfolios description to a different one"() {
    when:
      Portfolio created = portfolioApi.createPortfolio(new Portfolio().name("unique7").organizationId(org.getId()), Opts.empty(), superPerson)
    and:
      Portfolio update = new Portfolio().id(created.id).name(created.name).description("new desc")
    and:
      portfolioApi.updatePortfolio(update, Opts.empty())
    then:
      thrown PortfolioApi.DuplicatePortfolioException
  }

  def "we can create and delete a portfolio group"() {
    when:
      Portfolio created = portfolioApi.createPortfolio(new Portfolio().name("norton1").organizationId(org.getId()), Opts.empty(), superPerson)
    and: "i can delete it as well"
      portfolioApi.deletePortfolio(created.id)
    and: "now try and find it"
      Portfolio nowDeleted = portfolioApi.getPortfolio(created.id, Opts.empty(), superPerson)
    then:
      created != null
      nowDeleted == null
  }

  def "i don't have to provide a person when creating a portfolio because on initial setup i may not have one with oauth2"() {
    when: "i create a portfolio without a person, which is ok because of delayed oauth"
      Portfolio created = portfolioApi.createPortfolio(new Portfolio().name("norton2").organizationId(org.getId()), Opts.empty(), null)
    then: "it gets created, as its ok to not have a createdBy (for initial setup)"
      created != null
  }

  def "the person object must have a person id"() {
    when:
      Portfolio badUserNoId = portfolioApi.createPortfolio(new Portfolio().name("norton3").organizationId(org.getId()), Opts.empty(), new Person())
    then:
      thrown IllegalArgumentException
  }

  def "the person object must have a person id with an idXX"() {
    when:
      Portfolio badUserBadId = portfolioApi.createPortfolio(new Portfolio().name("norton4").organizationId(org.getId()), Opts.empty(), normalPerson)
    then:
      1 == 1
  }

  def "the person object must have a person id with an id"() {
    when:
      Portfolio badUserBadId2 = portfolioApi.createPortfolio(new Portfolio().name("norton5").organizationId(org.getId()), Opts.empty(), new Person(id: new PersonId(id: null)))
    then:
      thrown IllegalArgumentException
  }

  def "i can rename an existing portfolio"() {
    given: "i have a portfolio"
      def created = portfolioApi.createPortfolio(new Portfolio().name("norton6").organizationId(org.getId()), Opts.empty(), superPerson)
    when: "i rename the portfolio"
      def renamed = portfolioApi.updatePortfolio(created.name("new name").description("new desc"), Opts.empty())
    and: "i get it again"
      def found = portfolioApi.getPortfolio(created.id, Opts.empty(), superPerson)
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
      new QDbPortfolio().findList().each({ DbPortfolio p -> database.delete(p)})
    and: "i have three portfolios"
      portfolioApi.createPortfolio(new Portfolio().name("crispy").organizationId(org.getId()), Opts.empty(), superPerson)
      portfolioApi.createPortfolio(new Portfolio().name("crispy2").organizationId(org.getId()), Opts.empty(), superPerson)
      portfolioApi.createPortfolio(new Portfolio().name("fried").organizationId(org.getId()), Opts.empty(), superPerson)
    when: "i search for crisp"
      List<Portfolio> crispies = portfolioApi.findPortfolios("crisp", SortOrder.DESC, Opts.opts(FillOpts.Portfolios), superPerson)
    and: "i search for all"
      List<Portfolio> all = portfolioApi.findPortfolios(null,  SortOrder.ASC, Opts.opts(FillOpts.Members), superPerson)
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
