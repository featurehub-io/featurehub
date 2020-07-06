package io.featurehub.db.services

import io.ebean.DB
import io.ebean.Database
import io.featurehub.db.api.FillOpts
import io.featurehub.db.api.Opts
import io.featurehub.db.api.PortfolioApi
import io.featurehub.db.model.DbPerson
import io.featurehub.db.model.DbPortfolio
import io.featurehub.db.model.query.QDbPortfolio
import io.featurehub.db.publish.CacheSource
import io.featurehub.mr.model.Group
import io.featurehub.mr.model.Organization
import io.featurehub.mr.model.Person
import io.featurehub.mr.model.PersonId
import io.featurehub.mr.model.Portfolio
import io.featurehub.mr.model.SortOrder
import spock.lang.Shared
import spock.lang.Specification

class PortfolioSpec extends Specification {
  @Shared Database database
  @Shared ConvertUtils convertUtils
  @Shared PortfolioSqlApi portfolioApi
  @Shared GroupSqlApi groupSqlApi
  @Shared UUID superuser
  @Shared Organization org
  @Shared Person superPerson
  @Shared Person normalPerson

  def setupSpec() {
    System.setProperty("ebean.ddl.generate", "true")
    System.setProperty("ebean.ddl.run", "true")
    database = DB.getDefault()
    convertUtils = new ConvertUtils()
    def archiveStrategy = new DbArchiveStrategy(database, Mock(CacheSource))
    portfolioApi = new PortfolioSqlApi(database, convertUtils, archiveStrategy)
    groupSqlApi = new GroupSqlApi(database, convertUtils, archiveStrategy)
    // create the organization
    def orgApi = new OrganizationSqlApi(database, convertUtils)
    org = orgApi.get()
    if (org == null) {
      org = orgApi.save(new Organization().name("freddos"))
    }

    DbPerson user = Finder.findByEmail("irina@featurehub.io")
    if (user == null) {
      user = new DbPerson.Builder().email("irina@featurehub.io").name("Irina").build();
      database.save(user);
    }
    superuser = user.getId()
    superPerson = new Person().id(new PersonId().id(superuser.toString()))

    user = new DbPerson.Builder().email("portolio-simple@featurehub.io").build()
    database.save(user)
    normalPerson = new Person().id(new PersonId().id(user.id.toString()))

    Group adminGroup = groupSqlApi.findOrganizationAdminGroup(org.id, Opts.empty())
    if (adminGroup == null) {
      adminGroup = groupSqlApi.createOrgAdminGroup(org.id, 'Superuser groupo', superPerson)
    }

    groupSqlApi.addPersonToGroup(adminGroup.id, superPerson.id.id, Opts.empty())
  }

  def "we can create a portfolio group"() {
    when:
      Portfolio created = portfolioApi.createPortfolio(new Portfolio().name("norton").organizationId(org.getId()), Opts.empty(), superPerson)
    then:
      created != null
      portfolioApi.getPortfolio(created.getId(), Opts.empty(), superPerson).name == created.name
      portfolioApi.findPortfolios(created.name, org.id, SortOrder.ASC, Opts.empty(), superPerson).size() == 1
  }

  def "a normal person cannot see the portfolio group created by the admin"() {
    when: "superuser creates a group"
      Portfolio created = portfolioApi.createPortfolio(new Portfolio().name("normal-port-check").organizationId(org.getId()), Opts.empty(), superPerson)
    then:
      created != null
      portfolioApi.getPortfolio(created.id, Opts.empty(), normalPerson) == null
      portfolioApi.findPortfolios(created.name, org.id, SortOrder.ASC, Opts.empty(), normalPerson).size() == 0
  }

  def "a normal person in the portfolio group can see the portfolio"() {
    when: "superuser creates a group"
      Portfolio created = portfolioApi.createPortfolio(new Portfolio().name("normal-port-check1").organizationId(org.getId()), Opts.empty(), superPerson)
      Group group = groupSqlApi.createPortfolioGroup(created.id, new Group().name(created.name), superPerson)
      groupSqlApi.addPersonToGroup(group.id, normalPerson.id.id, Opts.empty())
    then:
      created != null
      portfolioApi.getPortfolio(created.id, Opts.empty(), normalPerson).name == created.name
      portfolioApi.findPortfolios(created.name, org.id, SortOrder.ASC, Opts.empty(), normalPerson).size() == 1
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

  def "a created portfolio must have a person who creates it"() {
    when: "i create a portfolio without a person"
      Portfolio created = portfolioApi.createPortfolio(new Portfolio().name("norton2").organizationId(org.getId()), Opts.empty(), null)
    and:
      Portfolio badUserNoId = portfolioApi.createPortfolio(new Portfolio().name("norton3").organizationId(org.getId()), Opts.empty(), new Person())
    and:
      Portfolio badUserBadId = portfolioApi.createPortfolio(new Portfolio().name("norton4").organizationId(org.getId()), Opts.empty(), new Person(id: new PersonId(id: "1")))
    and:
      Portfolio badUserBadId2 = portfolioApi.createPortfolio(new Portfolio().name("norton5").organizationId(org.getId()), Opts.empty(), new Person(id: new PersonId(id: null)))
    then:
      created == null
      badUserBadId == null
      badUserBadId2 == null
      badUserNoId == null
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
      Portfolio renamed = portfolioApi.updatePortfolio(new Portfolio().id(UUID.randomUUID().toString()).name("new name"), Opts.empty())
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
      List<Portfolio> crispies = portfolioApi.findPortfolios("crisp", org.getId(), SortOrder.DESC, Opts.opts(FillOpts.Portfolios), superPerson)
    and: "i search for all"
      List<Portfolio> all = portfolioApi.findPortfolios(null, org.getId(), SortOrder.ASC, Opts.opts(FillOpts.Members), superPerson)
    then:
      crispies.size() == 2
      crispies[0].name == 'crispy2' // should be sorted desc
      crispies[1].name == 'crispy'
      crispies*.whenCreated != null
      crispies*.createdBy*.id*.id as Set == [superuser.toString()] as Set
    and:
      all.size() >= 3
      all[0].name == 'crispy'
  }
}
