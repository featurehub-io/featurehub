package io.featurehub.db.services.strategies;

import io.featurehub.db.model.DbApplicationFeature;
import io.featurehub.db.model.DbFeatureValue;
import io.featurehub.mr.model.FeatureValue;
import io.featurehub.mr.model.RolloutStrategyInstance;

import java.util.List;

public interface StrategyDiffer {


  boolean invalidStrategyInstances(List<RolloutStrategyInstance> instances, DbApplicationFeature feature);

  boolean createDiff(FeatureValue featureValue, DbFeatureValue strategy);
}
