package io.featurehub.mr.rest;

import io.featurehub.db.api.FillOpts;
import io.featurehub.db.api.OptimisticLockingException;
import io.featurehub.db.api.Opts;
import io.featurehub.db.api.ServiceAccountApi;
import io.featurehub.mr.api.ServiceAccountSecuredService;
import io.featurehub.mr.auth.AuthManagerService;
import io.featurehub.mr.model.Person;
import io.featurehub.mr.model.ServiceAccount;
import io.featurehub.mr.model.ServiceAccountPermission;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Singleton
public class ServiceAccountResource implements ServiceAccountSecuredService {
  private static final Logger log = LoggerFactory.getLogger(ServiceAccountResource.class);
  private final AuthManagerService authManager;
  private final ServiceAccountApi serviceAccountApi;

  @Inject
  public ServiceAccountResource(AuthManagerService authManager, ServiceAccountApi serviceAccountApi) {
    this.authManager = authManager;
    this.serviceAccountApi = serviceAccountApi;
  }

  @Override
  public Boolean delete(String id, Boolean includePermissions, SecurityContext ctx) {
    Person person = authManager.from(ctx);

    if (authManager.isAnyAdmin(person)) {
      if (serviceAccountApi.delete(person, id)) {
        return true;
      };

      throw new NotFoundException();
    }

    throw new ForbiddenException();
  }

  @Override
  public ServiceAccount get(String id, Boolean includePermissions, SecurityContext ctx) {
    if ("self".equals(id)) {
      ServiceAccount account = authManager.serviceAccount(ctx);
      id = account.getId();
    } else {
      Person person = authManager.from(ctx);
      if (!authManager.isAnyAdmin(person)) {
        throw new ForbiddenException();
      }
    }

    ServiceAccount info = serviceAccountApi.get(id, new Opts().add(FillOpts.Permissions, includePermissions));

    if (info == null) {
      throw new NotFoundException();
    }

    return info;
  }

  @Override
  public ServiceAccount resetApiKey(String id, SecurityContext ctx) {
    Person person = authManager.from(ctx);

    if (authManager.isAnyAdmin(person)) {
      ServiceAccount sa = serviceAccountApi.resetApiKey(id);

      if (sa == null) {
        throw new NotFoundException();
      }

      return sa;
    }

    throw new ForbiddenException();
  }

  @Override
  public ServiceAccount update(String id, ServiceAccount serviceAccount, Boolean includePermissions, SecurityContext ctx) {
    Person person = authManager.from(ctx);

    Set<String> envIds =
      serviceAccount.getPermissions().stream().map(ServiceAccountPermission::getEnvironmentId).collect(Collectors.toSet());

    if (envIds.size() < serviceAccount.getPermissions().size()) {
      throw new BadRequestException("Duplicate environment ids were passed.");
    }

    if (authManager.isAnyAdmin(person)) {
      ServiceAccount result = null;

      try {
        result = serviceAccountApi.update(id, person, serviceAccount, new Opts().add(FillOpts.Permissions, includePermissions));
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
