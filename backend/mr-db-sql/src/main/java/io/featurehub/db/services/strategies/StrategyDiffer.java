package io.featurehub.db.services.strategies;

import io.featurehub.db.model.DbApplicationFeature;
import io.featurehub.db.model.DbFeatureValue;
import io.featurehub.db.model.DbStrategyForFeatureValue;
import io.featurehub.mr.model.Feature;
import io.featurehub.mr.model.FeatureValue;
import io.featurehub.mr.model.RolloutStrategyInstance;

import java.util.ArrayList;
import java.util.List;

public interface StrategyDiffer {

  class ChangedSharedStrategies {
    public List<DbStrategyForFeatureValue> deletedStrategies = new ArrayList<>();
    public List<DbStrategyForFeatureValue> updatedStrategies = new ArrayList<>();

    public boolean isEmpty() {
      return deletedStrategies.isEmpty() && updatedStrategies.isEmpty();
    }
  }

  boolean invalidStrategyInstances(List<RolloutStrategyInstance> instances, DbApplicationFeature feature);

  ChangedSharedStrategies createDiff(FeatureValue featureValue, DbFeatureValue strategy);
}
