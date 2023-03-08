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
import io.featurehub.mr.model.IdentityProviderInfo;
import io.featurehub.mr.model.Organization;
import io.featurehub.mr.model.Person;
import io.featurehub.mr.model.Portfolio;
import io.featurehub.mr.model.SetupMissingResponse;
import io.featurehub.mr.model.SetupResponse;
import io.featurehub.mr.model.SetupSiteAdmin;
import io.featurehub.mr.model.TokenizedPerson;
import io.featurehub.mr.utils.PortfolioUtils;
import io.featurehub.web.security.oauth.AuthProviderCollection;
import io.featurehub.web.security.oauth.AuthProviderSource;
import io.featurehub.web.security.oauth.providers.SSOProviderCustomisation;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.featurehub.utils.FallbackPropertyConfig;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
  private final AuthProviderCollection authProviderCollection;

  @ConfigKey("auth.disable-login")
  protected Boolean loginDisabled = Boolean.FALSE;

  @ConfigKey("ga.tracking-id")
  public String googleTrackingId = "";

  @Inject
  public SetupResource(SetupApi setupApi, AuthenticationApi authenticationApi, OrganizationApi organizationApi,
                       PortfolioApi portfolioApi, GroupApi groupApi, AuthenticationRepository authRepository,
                       PersonApi personApi, PortfolioUtils portfolioUtils, AuthProviderCollection authProviderCollection) {
    this.setupApi = setupApi;
    this.authenticationApi = authenticationApi;
    this.organizationApi = organizationApi;
    this.portfolioApi = portfolioApi;
    this.groupApi = groupApi;
    this.authRepository = authRepository;
    this.personApi = personApi;
    this.portfolioUtils = portfolioUtils;
    this.authProviderCollection = authProviderCollection;

    DeclaredConfigResolver.resolve(this);
  }

  @Override
  public SetupResponse isInstalled() {

    final List<String> providerCodes = new ArrayList<>(authProviderCollection.getCodes());

    if (!loginDisabled) {
      providerCodes.add("local");
    }

    if (setupApi.initialized()) {
      SetupResponse sr = new SetupResponse();
      sr.organization(organizationApi.get());

      sr.providers(providerCodes).providerInfo(fillProviderInfo());

      if (authProviderCollection.getProviders().size() == 1 && loginDisabled) { // only 1 external one
        final AuthProviderSource provider = authProviderCollection.getProviders().get(0);

        sr.redirectUrl(provider.getRedirectUrl());
      }

      boolean enricherEnabled = "true".equalsIgnoreCase(FallbackPropertyConfig.Companion.getConfig("enricher.enabled"
        , "true"));
      boolean webhooksEnabled = "true".equalsIgnoreCase(FallbackPropertyConfig.Companion.getConfig("webhooks.features.enabled"
        , "true"));

      sr.capabilityInfo(
        Map.of("webhook.features", (enricherEnabled && webhooksEnabled) ? "true" : "false" ,
          "trackingId", googleTrackingId)
      );

      return sr;
    }

    final SetupMissingResponse setupMissingResponse =
      new SetupMissingResponse()
        .capabilityInfo(Map.of("trackingId", googleTrackingId))
        .providers(providerCodes)
        .providerInfo(fillProviderInfo());


    throw new WebApplicationException(Response.status(Response.Status.NOT_FOUND).entity(setupMissingResponse).build());
  }

  private Map<String, IdentityProviderInfo> fillProviderInfo() {
    Map<String, IdentityProviderInfo> identityMap = new HashMap<>();

    authProviderCollection.getProviders()
      .forEach(am -> {
      if (am.getAuthInfo().getExposeOnLoginPage() && am.getAuthInfo().getIcon() != null) {
        final SSOProviderCustomisation icon = am.getAuthInfo().getIcon();
        identityMap.put(am.getCode(),
          new IdentityProviderInfo()
            .buttonIcon(icon.getIcon())
            .buttonBackgroundColor(icon.getButtonBackgroundColor())
            .buttonText(icon.getButtonText()));
      }
    });

    return identityMap;
  }

  @Override
  public TokenizedPerson setupSiteAdmin(SetupSiteAdmin setupSiteAdmin) {
    if (organizationApi.hasOrganisation()) {
      throw new WebApplicationException("duplicate", Response.Status.CONFLICT);
    }

    if (setupSiteAdmin.getPortfolio().trim().length() == 0) {
      throw new BadRequestException("Portfolio cannot be 0 length");
    }

    if (setupSiteAdmin.getOrganizationName().trim().length() == 0) {
      throw new BadRequestException("Org name cannot be 0 length");
    }

    // if we don't have an email address from them, and they haven't provided an auth provider in the setup request
    // we need to go and figure out what providers are available.
    if (setupSiteAdmin.getEmailAddress() == null) {
      if (setupSiteAdmin.getAuthProvider() == null) {
        if (authProviderCollection.getCodes().isEmpty()) {
          throw new BadRequestException("Cannot figure out how to authorise first user, no options provided.");
        }
        if (authProviderCollection.getCodes().size() > 1) {
          throw new BadRequestException("Cannot figure out how to authorise first user, too many options provided.");
        }

        setupSiteAdmin.setAuthProvider(authProviderCollection.getCodes().get(0));
      }

      AuthProviderSource ap = authProviderCollection.find(setupSiteAdmin.getAuthProvider());

      // they are using an external provider, so we can create the org and portfolio, but that is all.
      if (ap != null) {
        createPortfolio(setupSiteAdmin, createOrganization(setupSiteAdmin), null);

        return new TokenizedPerson().redirectUrl(ap.getRedirectUrl());
      } else {
        throw new BadRequestException("Unknown auth provider"); // invalid attempt to set up
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
        groupApi.createGroup(portfolio.getId(),
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
