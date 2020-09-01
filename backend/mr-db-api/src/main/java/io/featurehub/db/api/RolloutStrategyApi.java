package io.featurehub.db.api;

import io.featurehub.mr.model.Person;
import io.featurehub.mr.model.RolloutStrategy;
import io.featurehub.mr.model.RolloutStrategyInfo;

import java.util.List;

public interface RolloutStrategyApi {
  RolloutStrategyInfo createStrategy(String appId, RolloutStrategy rolloutStrategy, Person person, Opts opts) throws DuplicateNameException;

  RolloutStrategyInfo updateStrategy(String appId, RolloutStrategy rolloutStrategy, Person person, Opts opts) throws DuplicateNameException;

  List<RolloutStrategyInfo> listStrategies(String appId, boolean includeArchived, Opts opts);

  RolloutStrategyInfo getStrategy(String appId, String strategyIdOrName, Opts opts);

  RolloutStrategyInfo archiveStrategy(String appId, String strategyId, Person person, Opts add);

  static class DuplicateNameException extends Exception {
  }
}
