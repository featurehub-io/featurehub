package io.featurehub.mr.resources;

import cd.connect.app.config.ConfigKey;
import cd.connect.app.config.DeclaredConfigResolver;
import io.featurehub.db.api.AuthenticationApi;
import io.featurehub.db.api.FillOpts;
import io.featurehub.db.api.GroupApi;
import io.featurehub.db.api.Opts;
import io.featurehub.db.api.OrganizationApi;
import io.featurehub.db.api.PersonApi;
import io.featurehub.db.api.PortfolioApi;
import io.featurehub.db.api.SetupApi;
import io.featurehub.mr.api.SetupServiceDelegate;
import io.featurehub.mr.auth.AuthenticationRepository;
import io.featurehub.mr.model.Group;
import io.featurehub.mr.model.Organization;
import io.featurehub.mr.model.Person;
import io.featurehub.mr.model.Portfolio;
import io.featurehub.mr.model.SetupMissingResponse;
import io.featurehub.mr.model.SetupResponse;
import io.featurehub.mr.model.SetupSiteAdmin;
import io.featurehub.mr.model.TokenizedPerson;
import io.featurehub.web.security.oauth.AuthProvider;
import io.featurehub.mr.utils.PortfolioUtils;
import org.glassfish.hk2.api.IterableProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;

public class SetupResource implements SetupServiceDelegate {
  private static final Logger log = LoggerFactory.getLogger(SetupResource.class);
  private final SetupApi setupApi;
  private final AuthenticationApi authenticationApi;
  private final OrganizationApi organizationApi;
  private final PortfolioApi portfolioApi;
  private final GroupApi groupApi;
  private final AuthenticationRepository authRepository;
  private final PersonApi personApi;
  private final PortfolioUtils portfolioUtils;
  private final List<AuthProvider> authProviders;

  @ConfigKey("auth.disable-login")
  protected Boolean loginDisabled = Boolean.FALSE;

  @Inject
  public SetupResource(SetupApi setupApi, AuthenticationApi authenticationApi, OrganizationApi organizationApi,
                       PortfolioApi portfolioApi, GroupApi groupApi, AuthenticationRepository authRepository,
                       PersonApi personApi, PortfolioUtils portfolioUtils, IterableProvider<AuthProvider> authProviders) {
    this.setupApi = setupApi;
    this.authenticationApi = authenticationApi;
    this.organizationApi = organizationApi;
    this.portfolioApi = portfolioApi;
    this.groupApi = groupApi;
    this.authRepository = authRepository;
    this.personApi = personApi;
    this.portfolioUtils = portfolioUtils;
    this.authProviders = new ArrayList<AuthProvider>();
    authProviders.forEach(this.authProviders::add);

    DeclaredConfigResolver.resolve(this);
  }

  @Override
  public SetupResponse isInstalled() {
    if (setupApi.initialized()) {
      SetupResponse sr = new SetupResponse();
      sr.organization(organizationApi.get());

      // indicate back to login dialog what authentication providers are allowed

      authProviders.forEach(ap -> ap.getProviders().forEach(sr::addProvidersItem));
      boolean oneExternal = sr.getProviders().size() == 1;
      if (!loginDisabled) {
        sr.addProvidersItem("local");
      } else if (oneExternal) { // only 1 external one
        String providerName = sr.getProviders().get(0);
        authProviders.stream().filter(p -> p.getProviders().contains(providerName)).findFirst().ifPresent(ap ->
          sr.redirectUrl(ap.requestRedirectUrl(providerName)));
      }
      return sr;
    }

    final SetupMissingResponse setupMissingResponse = new SetupMissingResponse();
    authProviders.forEach(ap -> ap.getProviders().forEach(setupMissingResponse::addProvidersItem));
    if (!loginDisabled) {
      setupMissingResponse.addProvidersItem("local");
    }

    throw new WebApplicationException(Response.status(Response.Status.NOT_FOUND).entity(setupMissingResponse).build());
  }

  @Override
  public TokenizedPerson setupSiteAdmin(SetupSiteAdmin setupSiteAdmin) {
    Organization existingOrg = organizationApi.get();

    if (existingOrg != null) {
      throw new WebApplicationException("duplicate", Response.Status.CONFLICT);
    }

    if (setupSiteAdmin.getPortfolio().trim().length() == 0) {
      throw new BadRequestException("Portfolio cannot be 0 length");
    }

    if (setupSiteAdmin.getOrganizationName().trim().length() == 0) {
      throw new BadRequestException("Org name cannot be 0 length");
    }

    if (setupSiteAdmin.getEmailAddress() == null) {
      AuthProvider ap =
        authProviders.stream().filter(p -> p.getProviders().contains(setupSiteAdmin.getAuthProvider())).findFirst().orElse(null);

      // they are using an external provider, so we can create the org and portfolio, but that is all.
      if (ap != null) {
        createPortfolio(setupSiteAdmin, createOrganization(setupSiteAdmin), null);

        return new TokenizedPerson().redirectUrl(ap.requestRedirectUrl(setupSiteAdmin.getAuthProvider()));
      } else {
        throw new BadRequestException(); // invalid attempt to set up
      }
    }

    // normal non-external provider flow
    Organization organization = createOrganization(setupSiteAdmin);

    // create them
    try {
      personApi.create(setupSiteAdmin.getEmailAddress(), "Admin",null);
    } catch (PersonApi.DuplicatePersonException e) {
      throw new WebApplicationException(Response.status(Response.Status.CONFLICT).build());
    }

    // now register them
    Person person = authenticationApi.register(setupSiteAdmin.getName(), setupSiteAdmin.getEmailAddress(), setupSiteAdmin.getPassword(), null);

    createPortfolio(setupSiteAdmin, organization, person);

    //create the group and add admin to the group - any preference on group name here?
    Group group = groupApi.createOrgAdminGroup(organization.getId(), "org_admin", person);
    groupApi.addPersonToGroup(group.getId(), person.getId().getId(), Opts.empty());

    person = personApi.get(person.getId().getId(), Opts.opts(FillOpts.Groups, FillOpts.Acls));

    return new TokenizedPerson().accessToken(authRepository.put(person)).person(person);
  }

  private void createPortfolio(SetupSiteAdmin setupSiteAdmin, Organization organization, Person person) {
    //this should create portfolio
    try {
      Portfolio portfolio = portfolioApi.createPortfolio(new Portfolio().name(setupSiteAdmin.getPortfolio()).organizationId(organization.getId()), Opts.empty(), person);

      if (person != null) {
        groupApi.createPortfolioGroup(portfolio.getId(),
          new Group().name(portfolioUtils.formatPortfolioAdminGroupName(portfolio)).admin(true), person);
      }
    } catch (PortfolioApi.DuplicatePortfolioException | GroupApi.DuplicateGroupException e) {
      log.error("Duplicate portfolio name or group", e);
      throw new WebApplicationException(Response.Status.CONFLICT);
    }

  }

  protected Organization createOrganization(SetupSiteAdmin setupSiteAdmin) {
    // this should create the organisation
    return
      organizationApi.save(new Organization()
        .name(setupSiteAdmin.getOrganizationName()));
  }
}
