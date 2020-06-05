package io.featurehub.mr.rest;

import io.featurehub.db.api.*;
import io.featurehub.mr.api.InitializeSecuredService;
import io.featurehub.mr.auth.AuthenticationRepository;
import io.featurehub.mr.model.*;
import io.featurehub.mr.utils.PortfolioUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

@Singleton
public class InitializeResource implements InitializeSecuredService {
  private static final Logger log = LoggerFactory.getLogger(InitializeResource.class);
  private final SetupApi setupApi;
  private final AuthenticationApi authenticationApi;
  private final OrganizationApi organizationApi;
  private final PortfolioApi portfolioApi;
  private final GroupApi groupApi;
  private final AuthenticationRepository authRepository;
  private final PersonApi personApi;
  private final PortfolioUtils portfolioUtils;

  @Inject
  public InitializeResource(SetupApi setupApi, AuthenticationApi authenticationApi, OrganizationApi organizationApi, PortfolioApi portfolioApi, GroupApi groupApi, AuthenticationRepository authRepository, PersonApi personApi, PortfolioUtils portfolioUtils) {
    this.setupApi = setupApi;
    this.authenticationApi = authenticationApi;
    this.organizationApi = organizationApi;
    this.portfolioApi = portfolioApi;
    this.groupApi = groupApi;
    this.authRepository = authRepository;
    this.personApi = personApi;
    this.portfolioUtils = portfolioUtils;
  }

  @Override
  public Organization isInstalled() {
    if (setupApi.initialized()) {
      return organizationApi.get();
    }

    // deliberately throw a 404
    throw new NotFoundException();
  }

  @Override
  public TokenizedPerson setupSiteAdmin(SetupSiteAdmin setupSiteAdmin) {
    Organization existingOrg = organizationApi.get();

    if (existingOrg != null) {
      throw new WebApplicationException("duplicate", Response.Status.CONFLICT);
    }

    // create them
    try {
      personApi.create(setupSiteAdmin.getEmailAddress(), null);
    } catch (PersonApi.DuplicatePersonException e) {
      throw new WebApplicationException(Response.status(Response.Status.CONFLICT).build());
    }
    // now register them
    Person person = authenticationApi.register(setupSiteAdmin.getName(), setupSiteAdmin.getEmailAddress(), setupSiteAdmin.getPassword());

    // this should create the organisation
    Organization organization = organizationApi.save(new Organization().name(setupSiteAdmin.getOrganizationName()));

    //this should create portfolio
    try {
      Portfolio portfolio = portfolioApi.createPortfolio(new Portfolio().name(setupSiteAdmin.getPortfolio()).organizationId(organization.getId()), Opts.empty(), person);

      groupApi.createPortfolioGroup(portfolio.getId(),
        new Group().name(portfolioUtils.formatPortfolioAdminGroupName(portfolio)).admin(true), person);
    } catch (PortfolioApi.DuplicatePortfolioException | GroupApi.DuplicateGroupException e) {
      log.error("Duplicate portfolio name or group", e);
      throw new WebApplicationException(Response.Status.CONFLICT);
    }

    //create the group and add admin to the group - any preference on group name here?
    Group group = groupApi.createOrgAdminGroup(organization.getId(), "org_admin", person);
    groupApi.addPersonToGroup(group.getId(), person.getId().getId(), Opts.empty());

    person = personApi.get(person.getId().getId(), Opts.opts(FillOpts.Groups));

    return new TokenizedPerson().accessToken(authRepository.put(person)).person(person);
  }
}
