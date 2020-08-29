package io.featurehub.mr.resources;

import io.featurehub.mr.api.RolloutStrategyServiceDelegate;
import io.featurehub.mr.auth.AuthManagerService;
import io.featurehub.mr.model.Person;
import io.featurehub.mr.model.RolloutStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.SecurityContext;
import java.util.List;

public class RolloutStrategyResource implements RolloutStrategyServiceDelegate {
  private static final Logger log = LoggerFactory.getLogger(RolloutStrategyResource.class);
  private final AuthManagerService authManager;

  public RolloutStrategyResource(AuthManagerService authManager) {
    this.authManager = authManager;
  }

  @Override
  public RolloutStrategy createRolloutStrategy(String appId, RolloutStrategy rolloutStrategy, SecurityContext securityContext) {
    Person person = authManager.from(securityContext);
    return null;
  }

  @Override
  public Boolean deleteRolloutStrategy(String appId, String strategyId, SecurityContext securityContext) {
    Person person = authManager.from(securityContext);
    return null;
  }

  @Override
  public RolloutStrategy getRolloutStrategy(String appId, String strategyId, SecurityContext securityContext) {
    Person person = authManager.from(securityContext);
    return null;
  }

  @Override
  public List<RolloutStrategy> listApplicationRolloutStrategies(String appId, SecurityContext securityContext) {
    Person person = authManager.from(securityContext);
    return null;
  }

  @Override
  public RolloutStrategy updateRolloutStrategy(String appId, String strategyId, RolloutStrategy rolloutStrategy,
                                               SecurityContext securityContext) {
    Person person = authManager.from(securityContext);
    return null;
  }
}
