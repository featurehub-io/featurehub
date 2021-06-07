package io.featurehub.db.api;

import io.featurehub.mr.model.Person;
import io.featurehub.mr.model.RolloutStrategy;
import io.featurehub.mr.model.RolloutStrategyInfo;

import java.util.List;
import java.util.UUID;

public interface RolloutStrategyApi {
  RolloutStrategyInfo createStrategy(UUID appId, RolloutStrategy rolloutStrategy, Person person, Opts opts) throws DuplicateNameException;

  RolloutStrategyInfo updateStrategy(UUID appId, RolloutStrategy rolloutStrategy, Person person, Opts opts) throws DuplicateNameException;

  List<RolloutStrategyInfo> listStrategies(UUID appId, boolean includeArchived, Opts opts);

  RolloutStrategyInfo getStrategy(UUID appId, String strategyIdOrName, Opts opts);

  RolloutStrategyInfo archiveStrategy(UUID appId, String strategyIdOrName, Person person, Opts add);

  static class DuplicateNameException extends Exception {
  }
}
