package io.featurehub.mr.resources;

import io.featurehub.db.api.FillOpts;
import io.featurehub.db.api.Opts;
import io.featurehub.db.api.RolloutStrategyApi;
import io.featurehub.db.api.RolloutStrategyValidator;
import io.featurehub.mr.api.RolloutStrategyServiceDelegate;
import io.featurehub.mr.auth.AuthManagerService;
import io.featurehub.mr.model.CustomRolloutStrategyViolation;
import io.featurehub.mr.model.Person;
import io.featurehub.mr.model.RolloutStrategy;
import io.featurehub.mr.model.RolloutStrategyInfo;
import io.featurehub.mr.model.RolloutStrategyValidationRequest;
import io.featurehub.mr.model.RolloutStrategyValidationResponse;
import io.featurehub.mr.utils.ApplicationUtils;
import jakarta.inject.Inject;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.SecurityContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class RolloutStrategyResource implements RolloutStrategyServiceDelegate {
  private static final Logger log = LoggerFactory.getLogger(RolloutStrategyResource.class);
  private final ApplicationUtils applicationUtils;
  private final RolloutStrategyApi rolloutStrategyApi;
  private final RolloutStrategyValidator validator;

  @Inject
  public RolloutStrategyResource(AuthManagerService authManager, ApplicationUtils applicationUtils,
                                 RolloutStrategyApi rolloutStrategyApi, RolloutStrategyValidator validator) {
    this.applicationUtils = applicationUtils;
    this.rolloutStrategyApi = rolloutStrategyApi;
    this.validator = validator;
  }

  @Override
  public RolloutStrategyInfo createRolloutStrategy(UUID appId, RolloutStrategy rolloutStrategy,
                                                   CreateRolloutStrategyHolder holder,
                                                   SecurityContext securityContext) {
    Person person = applicationUtils.featureCreatorCheck(securityContext, appId).getCurrent();

    cleanStrategy(rolloutStrategy);

    final RolloutStrategyInfo strategy;

    try {
      strategy = rolloutStrategyApi.createStrategy(appId, rolloutStrategy, person,
        new Opts().add(FillOpts.SimplePeople, holder.includeWhoChanged));
    } catch (RolloutStrategyApi.DuplicateNameException e) {
      throw new WebApplicationException("Duplicate name", 409);
    }

    if (strategy == null) {
      throw new ForbiddenException();
    }

    return strategy;
  }

  @Override
  public RolloutStrategyInfo deleteRolloutStrategy(UUID appId, String strategyIdOrName, DeleteRolloutStrategyHolder holder,
                                                   SecurityContext securityContext) {
    Person person = applicationUtils.featureCreatorCheck(securityContext, appId).getCurrent();

    final RolloutStrategyInfo rolloutStrategyInfo = rolloutStrategyApi.archiveStrategy(appId, strategyIdOrName, person,
      new Opts().add(FillOpts.SimplePeople,
      holder.includeWhoChanged));

    if (rolloutStrategyInfo == null) {
      throw new NotFoundException();
    }

    return rolloutStrategyInfo;
  }

  @Override
  public RolloutStrategyInfo getRolloutStrategy(UUID appId, String strategyIdOrName, GetRolloutStrategyHolder holder,
                                                SecurityContext securityContext) {
    applicationUtils.featureReadCheck(securityContext, appId);

    RolloutStrategyInfo rs = rolloutStrategyApi.getStrategy(appId, strategyIdOrName, new Opts().add(FillOpts.SimplePeople,
      holder.includeWhoChanged));

    if (rs == null) {
      throw new NotFoundException();
    }

    return rs;
  }

  @Override
  public List<RolloutStrategyInfo> listApplicationRolloutStrategies(UUID appId,
                                                                    ListApplicationRolloutStrategiesHolder holder,
                                                                    SecurityContext securityContext) {
    applicationUtils.featureReadCheck(securityContext, appId);

    final List<RolloutStrategyInfo> strategies = rolloutStrategyApi.listStrategies(appId,
      Boolean.TRUE.equals(holder.includeArchived), new Opts().add(FillOpts.SimplePeople, holder.includeWhoChanged));

    if (strategies == null) {
      throw new NotFoundException(); // no such appId
    }

    return strategies;
  }

  @Override
  public RolloutStrategyInfo updateRolloutStrategy(UUID appId, String strategyIdOrName, RolloutStrategy rolloutStrategy,
                                                   UpdateRolloutStrategyHolder holder,
                                                   SecurityContext securityContext) {
    Person person = applicationUtils.featureCreatorCheck(securityContext, appId).getCurrent();

    cleanStrategy(rolloutStrategy);

    RolloutStrategyInfo strategy;
    try {
      strategy = rolloutStrategyApi.updateStrategy(appId, rolloutStrategy, person,
        new Opts().add(FillOpts.SimplePeople, holder.includeWhoChanged));

    } catch (RolloutStrategyApi.DuplicateNameException e) {
      throw new WebApplicationException("Duplicate name", 409);
    }

    if (strategy == null) {
      throw new ForbiddenException();
    }

    return strategy;
  }

  // we always clear the ids as we don't need them on saving, they are only used in the UI for validation tracking
  private void cleanStrategy(RolloutStrategy rs) {
    rs.getAttributes().forEach(attr -> attr.setId(null));
  }

  @Override
  public RolloutStrategyValidationResponse validate(UUID appId, RolloutStrategyValidationRequest req,
                                                    SecurityContext securityContext) {
    final RolloutStrategyValidator.ValidationFailure validationFailure =
      validator.validateStrategies(null ,req.getCustomStrategies(), req.getSharedStrategies());

    return
      new RolloutStrategyValidationResponse()
        .customStategyViolations(
          validationFailure.getCustomStrategyViolations().entrySet().stream().map(e ->
            new CustomRolloutStrategyViolation().strategy(e.getKey()).violations(new ArrayList<>(e.getValue()))
          ).collect(Collectors.toList()))
        .violations(new ArrayList<>(validationFailure.getCollectionViolationType()));
  }
}
