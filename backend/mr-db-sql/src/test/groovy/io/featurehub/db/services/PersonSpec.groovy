package io.featurehub.db.services


import io.featurehub.db.api.FillOpts
import io.featurehub.db.api.Opts
import io.featurehub.db.api.PersonApi
import io.featurehub.db.model.DbPerson
import io.featurehub.mr.model.Group
import io.featurehub.mr.model.Person
import io.featurehub.mr.model.PersonType
import io.featurehub.mr.model.Portfolio
import io.featurehub.mr.model.SortOrder
import org.apache.commons.lang3.RandomStringUtils
import spock.lang.Shared

class PersonSpec extends BaseSpec {
  @Shared PersonSqlApi personSqlApi
  @Shared PortfolioSqlApi portfolioSqlApi

  def setupSpec() {
    baseSetupSpec()

    personSqlApi = new PersonSqlApi(database, convertUtils, archiveStrategy, groupSqlApi)
    portfolioSqlApi = new PortfolioSqlApi(database, convertUtils, archiveStrategy)
  }

//  def "No created-by is passed when creating a user causes a null return"() {
//    when:
//      PersonApi.PersonToken p = personSqlApi.create("toddy1@f.com", null)
//    then:
//      p == null
//  }

  def "I can create a person"() {
    given: "i generate a random email"
      def email = RandomStringUtils.randomAscii(30).toLowerCase() + "@mailinator.com"
    when:
      def p = personSqlApi.create(email, "Babushka", superPerson.id.id)
    then:
      p.id != null
      personSqlApi.get(p.id, Opts.empty()).name == 'Babushka'
      personSqlApi.get(p.id, Opts.empty()).email == email
  }

  def "I can create a person with a null name"() {
    given: "i generate a random email"
      def email = RandomStringUtils.randomAscii(30).toLowerCase() + "@mailinator.com"
    when:
      def p = personSqlApi.create(email, null, superPerson.id.id)
    then:
      p != null
  }

//  def "No email is passed when creating a user causes a null return"() {
//    when:
//      PersonApi.PersonToken p = personSqlApi.create(null, "x", UUID.randomUUID())
//    then:
//      p == null
//  }

  def "Non uuid for createdBy causes a null return"() {
    when:
      PersonApi.PersonToken p = personSqlApi.create("toddy1@f.com", "y", UUID.randomUUID())
    then:
      p == null
  }

//  def "When I try and create a person and the created by has no id, I should get a null return"() {
//    when:
//      PersonApi.PersonToken p = personSqlApi.create("toddy1@f.com", null)
//    then:
//      p == null
//  }

  def "When I try and create a new person with a person who doesn't exist, I should get a null return"() {
    when:
      PersonApi.PersonToken p = personSqlApi.create("toddy1@f.com", "choppy", UUID.randomUUID())
    then:
      p == null
  }

  def "I can't register the same person twice"() {
    when: "i register"
      PersonApi.PersonToken p = personSqlApi.create("reg1@f.com", "z", superPerson.id.id)
    and: "i try and register the same email again"
      PersonApi.PersonToken p2 = personSqlApi.create("reg1@f.com", "z", superPerson.id.id)
    then:
      thrown PersonApi.DuplicatePersonException
  }

  def "When i try and find a person that doesn't exist, i get null"() {
    when:
      Person p = personSqlApi.get(UUID.randomUUID(), Opts.empty())
    then:
      p == null
  }

  def "When I try and find an invalid id person, i get null"() {
    when:
      Person p = personSqlApi.get(UUID.randomUUID(), Opts.empty())
    then:
      p == null
  }

  def "when i search for everyone, it will get limited"() {
    given:
      (1..30).each({it ->
        database.save(new DbPerson.Builder().email("$it-limited@me.com").name(String.format("limited %02d", it)).build())
      })
    when:
      PersonApi.PersonPagination p1 = personSqlApi.search('limited', null, 0, 10, Set.of(PersonType.PERSON), Opts.empty())
    and:
      PersonApi.PersonPagination p2 = personSqlApi.search('limited', SortOrder.ASC, 10, 10, Set.of(PersonType.PERSON), Opts.empty())
    and:
      PersonApi.PersonPagination p3 = personSqlApi.search('limited', SortOrder.DESC, 20, 10, Set.of(PersonType.PERSON), Opts.empty())
    and:
      PersonApi.PersonPagination p4 = personSqlApi.search('limited', null, 30, 10, Set.of(PersonType.PERSON), Opts.empty())
    then:
      p1.people.size() == 10
      p1.max == 30
      p2.people.size() == 10
      p2.max == 30
      p2.people[0].name == 'limited 11'
      p3.max == 30
      p3.people.size() == 10
      p3.people[0].name == 'limited 10' // the whole lot is sorted descending, not just the results
      p4.max == 30
      p4.people.size() == 0
  }

  def "when I search for a filter, i get the filter"() {
    given:
      (1..30).each({it ->
        database.save(new DbPerson.Builder().email("$it-filtered@me.com").name(String.format("filtered %02d", it)).build())
      })
    when:
      PersonApi.PersonPagination p1 = personSqlApi.search('filtered 0', SortOrder.ASC, 0, 20, Set.of(PersonType.PERSON), Opts.empty())
    then:
      p1.max == 9
      p1.people.size() == 9  // 01-09, 10, 20, 30
      p1.people.first().name == 'filtered 01'
      p1.people.last().name == 'filtered 09'
  }

  def "when I update a user, i get an updated user"() {
    given:
      def person = new DbPerson.Builder().email("updateme@me.com").name("update me").build()
      database.save(person)
    and: "i create two new portfolios"
      def p1 = portfolioSqlApi.createPortfolio(new Portfolio().name('upd-p-1').organizationId(org.id), Opts.empty(), superPerson)
      def p2 = portfolioSqlApi.createPortfolio(new Portfolio().name('upd-p-2').organizationId(org.id), Opts.empty(), superPerson)
    and: "i create two new groups"
      def g1 = groupSqlApi.createPortfolioGroup(p1.id, new Group(name: 'upd-g-1'), superPerson)
      def g2 = groupSqlApi.createPortfolioGroup(p2.id, new Group(name: 'upd-g-2'), superPerson)
    when:
      def originalPerson = personSqlApi.get(person.id, Opts.empty())
      def resultingPerson = personSqlApi.update(person.id,
        originalPerson.name("not me").email("updated@me.com").groups([g1, g2]), Opts.empty(), superuser)
    and:
      def addGroupsPerson = personSqlApi.get(person.id, Opts.opts(FillOpts.Groups))
    and:
      def removeGroupsPerson = personSqlApi.update(person.id,
        addGroupsPerson.copy().name("not you").email("updated@me.com").groups([g2]), Opts.empty(), superuser)
    and:
      def foundRemovedGroupsPerson = personSqlApi.get(person.id, Opts.opts(FillOpts.Groups))
    then:
      resultingPerson != null
      addGroupsPerson.name == 'not me'
      addGroupsPerson.email == 'updated@me.com'
      addGroupsPerson.groups.size() == 2
      addGroupsPerson.groups*.id.intersect([g1.id, g2.id]).size() == 2
      removeGroupsPerson != null
      foundRemovedGroupsPerson.name == 'not you'
      foundRemovedGroupsPerson.groups.size() == 1
      foundRemovedGroupsPerson.groups[0].id == g2.id
  }

  def "when I update the user as a portfolio admin, i can only modify my groups, but can change their name and email"() {
    given:
      def person = new DbPerson.Builder().email("updateme22@me.com").name("update me").build()
      database.save(person)
    and: "i create two new portfolios"
      def p1 = portfolioSqlApi.createPortfolio(new Portfolio().name('upd-p-a').organizationId(org.id), Opts.empty(), superPerson)
      def p2 = portfolioSqlApi.createPortfolio(new Portfolio().name('upd-p-b').organizationId(org.id), Opts.empty(), superPerson)
    and: "i create two new groups, one in each portfolio"
      def g1 = groupSqlApi.createPortfolioGroup(p1.id, new Group(name: 'upd-g-a'), superPerson)
      def g2 = groupSqlApi.createPortfolioGroup(p2.id, new Group(name: 'upd-g-b'), superPerson)
    and: "i create a portfolio admin group for portfolio 2"
      def gPortfolioAdmin = groupSqlApi.createPortfolioGroup(p2.id, new Group().name("admin of p2").admin(true), superPerson)
    and: "i create a user and make them a membe rof the portfolio admin group"
      def pAdmin = new DbPerson.Builder().name("Frederick Von Brinkenstorm").email("freddy@mailinator.com").build()
      database.save(pAdmin)
      def pAdminId = pAdmin.id
      groupSqlApi.addPersonToGroup(gPortfolioAdmin.id, pAdminId, Opts.empty())
    when: "the portfolio admin updates the person to add the two groups, but only have access to 1"
      def originalPerson = personSqlApi.get(person.id, Opts.empty())
      def resultingPerson = personSqlApi.update(person.id,
        originalPerson.name("not me").email("updated22@me.com").groups([g1, g2]), Opts.empty(), pAdminId)
      def addGroupsPerson = personSqlApi.get(person.id, Opts.opts(FillOpts.Groups))
    and: "then the superuser sets the groups to just g1"
      personSqlApi.update(person.id,
        addGroupsPerson.copy().name("not admin").email("updated22@me.com").groups([g1]), Opts.empty(), superuser)
      def adminUpdatePerson = personSqlApi.get(person.id, Opts.opts(FillOpts.Groups))
    and: "then portfolio admin tries to set just g2 on the user, which should give them g1 and g2"
      def removeGroupsPerson = personSqlApi.update(person.id,
        adminUpdatePerson.copy().name("not you").email("updated22@me.com").groups([g2]), Opts.empty(), pAdminId)
      def foundRemovedGroupsPerson = personSqlApi.get(person.id, Opts.opts(FillOpts.Groups))
    then:
      resultingPerson != null
      addGroupsPerson != null
      addGroupsPerson.groups*.id == [g2.id]
      adminUpdatePerson != null
      adminUpdatePerson.groups*.id == [g1.id]
      foundRemovedGroupsPerson.groups*.id.intersect([g1.id, g2.id]).size() == 2
      foundRemovedGroupsPerson.name == 'not you'
  }
}
