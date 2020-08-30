package io.featurehub.mr.resources;

import io.featurehub.db.api.FillOpts;
import io.featurehub.db.api.Opts;
import io.featurehub.db.api.RolloutStrategyApi;
import io.featurehub.mr.api.RolloutStrategyServiceDelegate;
import io.featurehub.mr.auth.AuthManagerService;
import io.featurehub.mr.model.Person;
import io.featurehub.mr.model.RolloutStrategy;
import io.featurehub.mr.model.RolloutStrategyInfo;
import io.featurehub.mr.utils.ApplicationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.ForbiddenException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.SecurityContext;
import java.util.List;

public class RolloutStrategyResource implements RolloutStrategyServiceDelegate {
  private static final Logger log = LoggerFactory.getLogger(RolloutStrategyResource.class);
  private final AuthManagerService authManager;
  private final ApplicationUtils applicationUtils;
  private final RolloutStrategyApi rolloutStrategyApi;

  public RolloutStrategyResource(AuthManagerService authManager, ApplicationUtils applicationUtils,
                                 RolloutStrategyApi rolloutStrategyApi) {
    this.authManager = authManager;
    this.applicationUtils = applicationUtils;
    this.rolloutStrategyApi = rolloutStrategyApi;
  }

  @Override
  public RolloutStrategyInfo createRolloutStrategy(String appId, RolloutStrategy rolloutStrategy,
                                                   CreateRolloutStrategyHolder holder,
                                                   SecurityContext securityContext) {
    applicationUtils.featureAdminCheck(securityContext, appId);
    Person person = authManager.from(securityContext);

    final RolloutStrategyInfo strategy;

    try {
      strategy = rolloutStrategyApi.createStrategy(appId, rolloutStrategy, person,
        new Opts().add(FillOpts.SimplePeople, holder.includeWhoChanged));
    } catch (RolloutStrategyApi.DuplicateNameException e) {
      throw new WebApplicationException("Duplicate name", 422);
    }

    if (strategy == null) {
      throw new ForbiddenException();
    }

    return strategy;
  }

  @Override
  public RolloutStrategyInfo deleteRolloutStrategy(String appId, String strategyId, DeleteRolloutStrategyHolder holder,
                               SecurityContext securityContext) {
    applicationUtils.featureAdminCheck(securityContext, appId);
    Person person = authManager.from(securityContext);
    return rolloutStrategyApi.archiveStrategy(appId, strategyId, person, new Opts().add(FillOpts.SimplePeople,
      holder.includeWhoChanged));
  }

  @Override
  public RolloutStrategyInfo getRolloutStrategy(String appId, String strategyId, GetRolloutStrategyHolder holder, SecurityContext securityContext) {
    applicationUtils.featureReadCheck(securityContext, appId);

    RolloutStrategyInfo rs = rolloutStrategyApi.getStrategy(appId, strategyId, new Opts().add(FillOpts.SimplePeople,
      holder.includeWhoChanged));

    if (rs == null) {
      throw new NotFoundException();
    }

    return rs;
  }

  @Override
  public List<RolloutStrategyInfo> listApplicationRolloutStrategies(String appId,
                                                                 ListApplicationRolloutStrategiesHolder holder, SecurityContext securityContext) {
    applicationUtils.featureReadCheck(securityContext, appId);

    final List<RolloutStrategyInfo> strategies = rolloutStrategyApi.listStrategies(appId,
      Boolean.TRUE.equals(holder.includeArchived), new Opts().add(FillOpts.SimplePeople, holder.includeWhoChanged));

    if (strategies == null) {
      throw new NotFoundException(); // no such appId
    }

    return strategies;
  }

  @Override
  public RolloutStrategyInfo updateRolloutStrategy(String appId, String strategyId, RolloutStrategy rolloutStrategy, UpdateRolloutStrategyHolder holder, SecurityContext securityContext)  {
    applicationUtils.featureAdminCheck(securityContext, appId);
    Person person = authManager.from(securityContext);

    RolloutStrategyInfo strategy;
    try {
      strategy = rolloutStrategyApi.updateStrategy(appId, rolloutStrategy, person, new Opts().add(FillOpts.SimplePeople, holder.includeWhoChanged));

    } catch (RolloutStrategyApi.DuplicateNameException e) {
      throw new WebApplicationException("Duplicate name", 422);
    }

    if (strategy == null) {
      throw new ForbiddenException();
    }

    return strategy;
  }
}
