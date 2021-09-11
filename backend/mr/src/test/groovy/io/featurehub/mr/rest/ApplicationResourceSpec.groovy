package io.featurehub.mr.rest

import io.featurehub.db.api.ApplicationApi
import io.featurehub.db.api.EnvironmentApi
import io.featurehub.db.api.FillOpts
import io.featurehub.db.api.GroupApi
import io.featurehub.db.api.Opts
import io.featurehub.db.api.OrganizationApi
import io.featurehub.db.api.PortfolioApi
import io.featurehub.db.api.ServiceAccountApi
import io.featurehub.mr.api.ApplicationServiceDelegate
import io.featurehub.mr.auth.AuthManagerService
import io.featurehub.mr.model.Application
import io.featurehub.mr.model.Environment
import io.featurehub.mr.model.Person
import io.featurehub.mr.model.SortOrder
import io.featurehub.mr.resources.ApplicationResource
import io.featurehub.mr.utils.ApplicationUtils
import jakarta.ws.rs.ForbiddenException
import jakarta.ws.rs.core.SecurityContext
import spock.lang.Specification

class ApplicationResourceSpec extends Specification {
  AuthManagerService authManager;
  ApplicationApi applicationApi;
  GroupApi groupApi;
  PortfolioApi portfolioApi;
  OrganizationApi organizationApi;
  ApplicationResource ar
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

    ar = new ApplicationResource(authManager, applicationApi, environmentApi, new ApplicationUtils(authManager, applicationApi))
  }

  def "if you are not a portfolio admin you cannot create an application"() {
    when: "i attempt to create an application"
      ar.createApplication(UUID.randomUUID(), new Application(), new ApplicationServiceDelegate.CreateApplicationHolder(), null)
    then:
      thrown ForbiddenException
  }

  def "if you are a portfolio admin you can create an application"() {
    given: "i am a portfolio admin"
      SecurityContext sc = Mock(SecurityContext)
      Person person = new Person()
      authManager.from(sc) >> person
      UUID pId = UUID.randomUUID()
      UUID appId = UUID.randomUUID()
      authManager.isPortfolioAdmin(pId, person, null) >> true
    and: "i have an application"
      Application app = new Application()
    when: "i attempt to create an application"
      ar.createApplication(pId, app, new ApplicationServiceDelegate.CreateApplicationHolder(), sc)
    then:
      1 * applicationApi.createApplication(pId, app, person) >> new Application().id(appId)
      1 * environmentApi.create({ Environment e -> e.applicationId == appId}, { Application a -> a.id == appId}, _)
  }

  def "findApplications works"() {
    given:
      SecurityContext sc = Mock(SecurityContext)
      UUID portfolioId = UUID.randomUUID()
      Person person = new Person()
      authManager.from(sc) >> person
      authManager.isOrgAdmin(person) >> true
    when: "i find applications"
      ar.findApplications(portfolioId, new ApplicationServiceDelegate.FindApplicationsHolder(includeEnvironments: true, order: SortOrder.ASC, filter: "fred"), sc)
    then:
      1 * applicationApi.findApplications(portfolioId, "fred", SortOrder.ASC, Opts.opts(FillOpts.Environments), person, true) >> []
  }
}
