package io.featurehub.mr.rest

import io.featurehub.db.api.ApplicationApi
import io.featurehub.db.api.EnvironmentApi
import io.featurehub.db.api.FillOpts
import io.featurehub.db.api.GroupApi
import io.featurehub.db.api.Opts
import io.featurehub.db.api.OrganizationApi
import io.featurehub.db.api.PortfolioApi
import io.featurehub.db.api.ServiceAccountApi
import io.featurehub.mr.auth.AuthManagerService
import io.featurehub.mr.model.Application
import io.featurehub.mr.model.Environment
import io.featurehub.mr.model.Group
import io.featurehub.mr.model.Person
import io.featurehub.mr.model.Portfolio
import io.featurehub.mr.model.SortOrder
import io.featurehub.mr.utils.PortfolioUtils
import spock.lang.Specification

import javax.ws.rs.ForbiddenException
import javax.ws.rs.NotFoundException
import javax.ws.rs.WebApplicationException
import javax.ws.rs.core.Response
import javax.ws.rs.core.SecurityContext

class PortfolioResourceSpec extends Specification {
  AuthManagerService authManager;
  ApplicationApi applicationApi;
  GroupApi groupApi;
  PortfolioApi portfolioApi;
  OrganizationApi organizationApi;
  PortfolioResource pr
  ServiceAccountApi serviceAccountApi
  EnvironmentApi environmentApi

  def setup() {
    authManager = Mock(AuthManagerService)
    applicationApi = Mock(ApplicationApi)
    groupApi = Mock(GroupApi)
    portfolioApi = Mock(PortfolioApi)
    organizationApi = Mock(OrganizationApi)
    serviceAccountApi = Mock(ServiceAccountApi)
    environmentApi = Mock(EnvironmentApi)

    System.setProperty('portfolio.admin.group.suffix', 'Administrators')

    pr = new PortfolioResource(authManager, applicationApi, groupApi, portfolioApi, organizationApi, serviceAccountApi, new PortfolioUtils(), environmentApi)
  }

  def "if you are not a portfolio admin you cannot create an application"() {
    when: "i attempt to create an application"
      pr.createApplication("1", new Application(), null, null)
    then:
      thrown ForbiddenException
  }

  def "if you are a portfolio admin you can create an application"() {
    given: "i am a portfolio admin"
      SecurityContext sc = Mock(SecurityContext)
      Person person = new Person()
      authManager.from(sc) >> person
      String pId = "1"
      authManager.isPortfolioAdmin(pId, person, null) >> true
    and: "i have an application"
      Application app = new Application()
    when: "i attempt to create an application"
      pr.createApplication(pId, app, null, sc)
    then:
      1 * applicationApi.createApplication(pId, app, person) >> new Application().id("fred")
      1 * environmentApi.create({ Environment e -> e.applicationId == "fred"}, { Application a -> a.id == "fred"}, _)
  }

  def "if you are a portfolio admin you can create a group"() {
    given: "i am a portfolio admin"
      SecurityContext sc = Mock(SecurityContext)
      Person person = new Person()
      authManager.from(sc) >> person
      String pId = "1"
      authManager.isPortfolioAdmin(pId, person, null) >> true
    and: "i have a group"
      Group group = new Group()
    when: "i attempt to create said Group"
      pr.createGroup("1", group, true, sc)
    then:
      1 * groupApi.createPortfolioGroup(pId, group, person)
  }

  def "if you are not a portfolio admin, you cannot create a group"() {
    given: "i am not a portfolio admin"
      SecurityContext sc = Mock(SecurityContext)
      Person person = new Person()
      authManager.from(sc) >> person
      String pId = "1"
      authManager.isPortfolioAdmin(pId, person, null) >> false
    and: "i have a group"
      Group group = new Group()
    when: "i attempt to create a group"
      pr.createGroup("1", group, true, sc)
    then:
      thrown(ForbiddenException)
  }

  def "if you are not an org admin, you cannot create a portfolio"() {
    given: "i am not an org admin"
      SecurityContext sc = Mock(SecurityContext)
      Person person = new Person()
      authManager.from(sc) >> person
      authManager.isOrgAdmin(person) >> false
    when: "i try and create a portfolio"
      pr.createPortfolio(new Portfolio().name("art"), false, false, sc)
    then:
      thrown(ForbiddenException)
  }

  def "if you are an org admin, you can create a portfolio"() {
    given: "i am an org admin"
      SecurityContext sc = Mock(SecurityContext)
      Person person = new Person()
      authManager.from(sc) >> person
      authManager.isOrgAdmin(person) >> true
    and: "i have a portfolio"
      Portfolio p = new Portfolio().name("art")
    when: "i try and create a portfolio"
      pr.createPortfolio(p, false, false, sc)
    then:
      1 * portfolioApi.createPortfolio(p, (Opts)_, person) >> new Portfolio()
//      1 * groupApi.addPersonToGroup(*_) > new Group()
  }

  def "an org admin cannot create a duplicate portfolio"() {
    given: "i am an org admin"
      authManager.isOrgAdmin(_) >> true
    and: "i create a new duplicate portfolio"
      Portfolio p = new Portfolio()
    when: "i try and create a portfolio"
      pr.createPortfolio(p, false, false, null)
    then:
      1 * portfolioApi.createPortfolio(p, (Opts)_, _) >> { throw new PortfolioApi.DuplicatePortfolioException() }
      WebApplicationException ex = thrown()
      ex.response.status == Response.Status.CONFLICT.statusCode
  }

  def "only an org admin can delete a portfolio"() {
    given: "i am not an org admin"
      SecurityContext sc = Mock(SecurityContext)
      Person person = new Person()
      authManager.from(sc) >> person
      authManager.isOrgAdmin(person) >> false
    when: "i try and delete a portfolio"
      pr.deletePortfolio(UUID.randomUUID().toString(), true, true, true, sc)
    then:
      thrown(ForbiddenException)
  }

  def "you can only delete a portfolio that exists"() {
    given: "i am an org admin"
      SecurityContext sc = Mock(SecurityContext)
      Person person = new Person()
      authManager.from(sc) >> person
      authManager.isOrgAdmin(person) >> true
    when: "i try and delete a portfolio"
      def count = pr.deletePortfolio(UUID.randomUUID().toString(), true, true, true, sc)
    then:
      !count
  }

  def "an org admin can delete a portfolio"() {
    given: "i am an org admin"
      SecurityContext sc = Mock(SecurityContext)
      Person person = new Person()
      authManager.from(sc) >> person
      authManager.isOrgAdmin(person) >> true
    and: "i create a portfolio"
      String pId = "1"
      portfolioApi.getPortfolio(pId, (Opts)_, _) >> new Portfolio()
    when: "i try and delete a portfolio"
      def count = pr.deletePortfolio(pId, true, true, true, sc)
    then:
      count
  }

  def "findApplications works"() {
    given:
      SecurityContext sc = Mock(SecurityContext)
      Person person = new Person()
      authManager.from(sc) >> person
      authManager.isOrgAdmin(person) >> true
    when: "i find applications"
      pr.findApplications("1", true, SortOrder.ASC, "fred", sc)
    then:
      1 * applicationApi.findApplications("1", "fred", SortOrder.ASC, Opts.opts(FillOpts.Environments), person, true) >> []
  }

  def "findGroups works"() {
    given:
      SecurityContext sc = Mock(SecurityContext)
      Person person = new Person()
      authManager.from(sc) >> person
      authManager.isOrgAdmin(person) >> true
    when: "i find groups"
      pr.findGroups("1", true, SortOrder.DESC, "turn", sc)
    then:
      1 * groupApi.findGroups("1", "turn", SortOrder.DESC, Opts.opts(FillOpts.People))
  }

  def "getting a non-existent portfolio throws 404"() {
    when:
      pr.getPortfolio("1", true, true, true, null)
    then:
      thrown(NotFoundException)
  }

  def "getting portfolio returns portfolio"() {
    given: "i have a portfolio"
      Portfolio p = new Portfolio().id("sheep")
      portfolioApi.getPortfolio("1", (Opts)_, _) >> p
    when:
      Portfolio p1 = pr.getPortfolio("1", true, true, true, null)
    then:
      p1 != null
      p1.id == 'sheep'
  }

  def "renaming portfolio is allowed to portfolio admins"() {
    given: "i setup the portfolio"
      String pId = "x"
      Portfolio p = new Portfolio().id(pId)
      portfolioApi.updatePortfolio(p, (Opts)_) >> p
    and: "i am a person with admin rights"
      SecurityContext sc = Mock(SecurityContext)

      Person person = new Person()
      authManager.from(sc) >> person
      authManager.isPortfolioAdmin(pId, person, null) >> true
    when: "i rename the portfolio"
      pr.updatePortfolio(pId, p, true, true, true, sc)
    then:
      1 * portfolioApi.updatePortfolio(p, (Opts)_) >> p
  }

  def "renaming a portfolio to the name of an existing portfolio results in a conflict error"() {
    given: "i am an admin"
      SecurityContext sc = Mock(SecurityContext)
      String portfolioId = "x"
      Person person = new Person()
      authManager.from(sc) >> person
      authManager.isPortfolioAdmin(portfolioId, person, null) >> true
    and:
      Portfolio p = new Portfolio()
      portfolioApi.updatePortfolio(p, _) >> { throw new PortfolioApi.DuplicatePortfolioException() }
    when:
      pr.updatePortfolio(portfolioId, p, null, null, true, sc)
    then:
      WebApplicationException ex = thrown()
      ex.response.status == Response.Status.CONFLICT.statusCode
  }

  def "renaming portfolios is not allowed to non portfolio admins"() {
    given: "i setup the portfolio"
      Portfolio p = new Portfolio().id("x")
      portfolioApi.updatePortfolio(p, (Opts)_) >> p
    and: "i am a person"
      SecurityContext sc = Mock(SecurityContext)
      Person person = new Person()
      authManager.from(sc) >> person
    when: "i rename the portfolio"
      pr.updatePortfolio("1", p, true, true, true, sc)
    then:
      thrown(ForbiddenException)
  }

  def "cannot rename a non-existent portfolio"() {
    given: "i setup the portfolio"
      String pId = "y"
      Portfolio p = new Portfolio().id(pId)
      portfolioApi.updatePortfolio(p, (Opts)_) >> null
    and: "i am a person with admin rights"
      SecurityContext sc = Mock(SecurityContext)

      Person person = new Person()
      authManager.from(sc) >> person
      authManager.isPortfolioAdmin(pId, person, null) >> true
    when: "i rename the portfolio"
      pr.updatePortfolio(pId, p, true, true, true, sc)
    then:
      thrown(NotFoundException)
  }
}
