package io.featurehub.mr.resources;

import io.featurehub.db.FilterOptType;
import io.featurehub.db.api.FillOpts;
import io.featurehub.db.api.OptimisticLockingException;
import io.featurehub.db.api.Opts;
import io.featurehub.db.api.ServiceAccountApi;
import io.featurehub.mr.api.ServiceAccountServiceDelegate;
import io.featurehub.mr.auth.AuthManagerService;
import io.featurehub.mr.model.Person;
import io.featurehub.mr.model.ResetApiKeyType;
import io.featurehub.mr.model.ServiceAccount;
import io.featurehub.mr.model.ServiceAccountPermission;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class ServiceAccountResource implements ServiceAccountServiceDelegate {
  private static final Logger log = LoggerFactory.getLogger(ServiceAccountResource.class);
  private final AuthManagerService authManager;
  private final ServiceAccountApi serviceAccountApi;

  @Inject
  public ServiceAccountResource(AuthManagerService authManager, ServiceAccountApi serviceAccountApi) {
    this.authManager = authManager;
    this.serviceAccountApi = serviceAccountApi;
  }

  @Override
  public ServiceAccount createServiceAccountInPortfolio(UUID id, ServiceAccount serviceAccount,
                                                        CreateServiceAccountInPortfolioHolder holder, SecurityContext securityContext) {
    Person person = authManager.from(securityContext);

    if (authManager.isPortfolioAdmin(id, person) || authManager.isOrgAdmin(person)) {
      try {
        return serviceAccountApi.create(id, person, serviceAccount, new Opts().add(FillOpts.Permissions, holder.includePermissions));
      } catch (ServiceAccountApi.DuplicateServiceAccountException e) {
        log.warn("Attempt to create duplicate service account {}", serviceAccount.getName());
        throw new WebApplicationException(Response.Status.CONFLICT);
      }
    }

    throw new ForbiddenException();
  }

  @Override
  public Boolean deleteServiceAccount(UUID id, DeleteServiceAccountHolder holder, SecurityContext securityContext) {
    Person person = authManager.from(securityContext);

    if (authManager.isPortfolioAdmin(id, person) || authManager.isOrgAdmin(person)) {
      if (serviceAccountApi.delete(person, id)) {
        return true;
      };

      throw new NotFoundException();
    }

    throw new ForbiddenException();
  }

  @Override
  public ServiceAccount getServiceAccount(UUID id, GetServiceAccountHolder holder, SecurityContext securityContext) {
    if ("self".equals(id)) {
      ServiceAccount account = authManager.serviceAccount(securityContext);
      id = account.getId();
    }

    ServiceAccount info = serviceAccountApi.get(id,
      new Opts().add(FillOpts.Permissions, holder.includePermissions).add(FilterOptType.Application, holder.byApplicationId));

    if (info == null) {
      throw new NotFoundException();
    }

    Person person = authManager.from(securityContext);
    if (!authManager.isPortfolioAdmin(info.getPortfolioId(), person) && !authManager.isOrgAdmin(person)) {
      throw new ForbiddenException();
    }


    return info;
  }

  @Override
  public ServiceAccount resetApiKey(UUID id, ResetApiKeyHolder holder, SecurityContext securityContext) {
    Person person = authManager.from(securityContext);

    ServiceAccount info = serviceAccountApi.get(id,  Opts.empty());

    if (info == null) {
      throw new NotFoundException();
    }

    if (!authManager.isPortfolioAdmin(info.getPortfolioId(), person) && !authManager.isOrgAdmin(person)) {
      throw new ForbiddenException();
    }

    ServiceAccount sa = serviceAccountApi.resetApiKey(id,
      holder.apiKeyType != ResetApiKeyType.SERVER_EVAL_ONLY,
      holder.apiKeyType != ResetApiKeyType.CLIENT_EVAL_ONLY);

    if (sa == null) {
      throw new NotFoundException();
    }

    return sa;
  }

  @Override
  public List<ServiceAccount> searchServiceAccountsInPortfolio(UUID id, SearchServiceAccountsInPortfolioHolder holder,
                                                               SecurityContext securityContext) {
    Person person = authManager.from(securityContext);

    List<ServiceAccount> serviceAccounts = serviceAccountApi.search(id, holder.filter, holder.applicationId,
          person,
          new Opts().add(FillOpts.ServiceAccountPermissionFilter)
            .add(FillOpts.Permissions, holder.includePermissions)
            .add(FillOpts.SdkURL, holder.includeSdkUrls));

    if (serviceAccounts == null) {
      return new ArrayList<>();
    }

    serviceAccounts.sort(Comparator.comparing(ServiceAccount::getName));

    return serviceAccounts;
  }

  @Override
  public ServiceAccount updateServiceAccount(UUID serviceAccountId, ServiceAccount serviceAccount,
                                             UpdateServiceAccountHolder holder, SecurityContext securityContext) {
    Person person = authManager.from(securityContext);

    Set<UUID> envIds =
      serviceAccount.getPermissions().stream().map(ServiceAccountPermission::getEnvironmentId).collect(Collectors.toSet());

    if (envIds.size() < serviceAccount.getPermissions().size()) {
      throw new BadRequestException("Duplicate environment ids were passed.");
    }

    if (serviceAccount.getPortfolioId() == null) {
      throw new BadRequestException("No portfolio passed");
    }

    if (authManager.isPortfolioAdmin(serviceAccount.getPortfolioId(), person) || authManager.isOrgAdmin(person) ) {
      ServiceAccount result = null;

      try {
        result = serviceAccountApi.update(serviceAccountId, person, serviceAccount, new Opts().add(FillOpts.Permissions, holder.includePermissions));
      } catch (OptimisticLockingException e) {
        throw new WebApplicationException(422);
      }

      if (result == null) {
        throw new NotFoundException();
      }

      return result;
    }

    throw new ForbiddenException();
  }
}
