package io.featurehub.mr.rest

import io.featurehub.db.api.ApplicationApi
import io.featurehub.db.api.EnvironmentApi
import io.featurehub.db.api.GroupApi
import io.featurehub.db.api.Opts
import io.featurehub.db.api.OrganizationApi
import io.featurehub.db.api.PortfolioApi
import io.featurehub.db.api.ServiceAccountApi
import io.featurehub.mr.api.PortfolioServiceDelegate
import io.featurehub.mr.auth.AuthManagerService
import io.featurehub.mr.model.Person
import io.featurehub.mr.model.Portfolio
import io.featurehub.mr.resources.PortfolioResource
import io.featurehub.mr.utils.PortfolioUtils
import jakarta.ws.rs.ForbiddenException
import jakarta.ws.rs.NotFoundException
import jakarta.ws.rs.WebApplicationException
import jakarta.ws.rs.core.Response
import jakarta.ws.rs.core.SecurityContext
import spock.lang.Specification

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

    pr = new PortfolioResource(groupApi, authManager, portfolioApi, organizationApi, new PortfolioUtils())
  }


  def "if you are not an org admin, you cannot create a portfolio"() {
    given: "i am not an org admin"
      SecurityContext sc = Mock(SecurityContext)
      Person person = new Person()
      authManager.from(sc) >> person
      authManager.isOrgAdmin(person) >> false
    when: "i try and create a portfolio"
      pr.createPortfolio(new Portfolio().name("art"), new PortfolioServiceDelegate.CreatePortfolioHolder(), sc)
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
      pr.createPortfolio(p, new PortfolioServiceDelegate.CreatePortfolioHolder(), sc)
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
      pr.createPortfolio(p, new PortfolioServiceDelegate.CreatePortfolioHolder(), null)
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
      pr.deletePortfolio(UUID.randomUUID(), new PortfolioServiceDelegate.DeletePortfolioHolder(), sc)
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
      def count = pr.deletePortfolio(UUID.randomUUID(), new PortfolioServiceDelegate.DeletePortfolioHolder(), sc)
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
      UUID pId = UUID.randomUUID()
      portfolioApi.getPortfolio(pId, (Opts)_, _) >> new Portfolio()
    when: "i try and delete a portfolio"
      def count = pr.deletePortfolio(pId, new PortfolioServiceDelegate.DeletePortfolioHolder(), sc)
    then:
      count
  }

  def "getting a non-existent portfolio throws 404"() {
    when:
      pr.getPortfolio(UUID.randomUUID(), new PortfolioServiceDelegate.GetPortfolioHolder(includeEnvironments: true, includeApplications: true, includeGroups: true), null)
    then:
      thrown(NotFoundException)
  }

  def "getting portfolio returns portfolio"() {
    given: "i have a portfolio"
      UUID portId = UUID.randomUUID()
      Portfolio p = new Portfolio().id(portId)
      portfolioApi.getPortfolio(portId, (Opts)_, _) >> p
    when:
      Portfolio p1 = pr.getPortfolio(portId, new PortfolioServiceDelegate.GetPortfolioHolder(includeEnvironments: true, includeApplications: true, includeGroups: true), null)
    then:
      p1 != null
      p1.id == portId
  }

  def "renaming portfolio is allowed to portfolio admins"() {
    given: "i setup the portfolio"
      UUID pId = UUID.randomUUID()
      Portfolio p = new Portfolio().id(pId).name("name")
      portfolioApi.updatePortfolio(p, (Opts)_) >> p
    and: "i am a person with admin rights"
      SecurityContext sc = Mock(SecurityContext)

      Person person = new Person()
      authManager.from(sc) >> person
      authManager.isPortfolioAdmin(pId, person, null) >> true
    when: "i rename the portfolio"
      pr.updatePortfolio(pId, p, new PortfolioServiceDelegate.UpdatePortfolioHolder(), sc)
    then:
      1 * portfolioApi.updatePortfolio(p, (Opts)_) >> p
  }

  def "renaming a portfolio to the name of an existing portfolio results in a conflict error"() {
    given: "i am an admin"
      SecurityContext sc = Mock(SecurityContext)
      UUID portfolioId = UUID.randomUUID()
      Person person = new Person()
      authManager.from(sc) >> person
      authManager.isPortfolioAdmin(portfolioId, person, null) >> true
    and:
      Portfolio p = new Portfolio()
      portfolioApi.updatePortfolio(p, _) >> { throw new PortfolioApi.DuplicatePortfolioException() }
    when:
      pr.updatePortfolio(portfolioId, p, new PortfolioServiceDelegate.UpdatePortfolioHolder(), sc)
    then:
      WebApplicationException ex = thrown()
      ex.response.status == Response.Status.CONFLICT.statusCode
  }

  def "renaming portfolios is not allowed to non portfolio admins"() {
    given: "i setup the portfolio"
      Portfolio p = new Portfolio().id(UUID.randomUUID())
      portfolioApi.updatePortfolio(p, (Opts)_) >> p
    and: "i am a person"
      SecurityContext sc = Mock(SecurityContext)
      Person person = new Person()
      authManager.from(sc) >> person
    when: "i rename the portfolio"
      pr.updatePortfolio(p.id, p, new PortfolioServiceDelegate.UpdatePortfolioHolder(), sc)
    then:
      thrown(ForbiddenException)
  }

  def "cannot rename a non-existent portfolio"() {
    given: "i setup the portfolio"
      UUID pId = UUID.randomUUID()
      Portfolio p = new Portfolio().id(pId)
      portfolioApi.updatePortfolio(p, (Opts)_) >> null
    and: "i am a person with admin rights"
      SecurityContext sc = Mock(SecurityContext)

      Person person = new Person()
      authManager.from(sc) >> person
      authManager.isPortfolioAdmin(pId, person, null) >> true
    when: "i rename the portfolio"
      pr.updatePortfolio(pId, p, new PortfolioServiceDelegate.UpdatePortfolioHolder(), sc)
    then:
      thrown(NotFoundException)
  }
}
