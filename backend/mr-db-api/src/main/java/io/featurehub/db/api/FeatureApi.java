package io.featurehub.db.api;

import io.featurehub.mr.model.ApplicationFeatureValues;
import io.featurehub.mr.model.EnvironmentFeaturesResult;
import io.featurehub.mr.model.FeatureEnvironment;
import io.featurehub.mr.model.FeatureValue;
import io.featurehub.mr.model.Person;

import java.util.List;
import java.util.UUID;

public interface FeatureApi {

  void updateAllFeatureValuesByApplicationForKey(UUID id, String key, List<FeatureValue> featureValue, Person from,
                                                 boolean removeValuesNotPassed)
    throws OptimisticLockingException, NoAppropriateRole,
    RolloutStrategyValidator.InvalidStrategyCombination;

  ApplicationFeatureValues findAllFeatureAndFeatureValuesForEnvironmentsByApplication(UUID appId, Person current);

  class NoAppropriateRole extends Exception {
  }

  FeatureValue createFeatureValueForEnvironment(UUID eid, String key, FeatureValue featureValue,
                                                PersonFeaturePermission person) throws OptimisticLockingException,
    RolloutStrategyValidator.InvalidStrategyCombination, NoAppropriateRole;

  boolean deleteFeatureValueForEnvironment(UUID eid, String key);

  FeatureValue updateFeatureValueForEnvironment(UUID eid, String key, FeatureValue featureValue,
                                                PersonFeaturePermission person) throws OptimisticLockingException,
    RolloutStrategyValidator.InvalidStrategyCombination, NoAppropriateRole;

  FeatureValue getFeatureValueForEnvironment(UUID eid, String key);

  EnvironmentFeaturesResult getAllFeatureValuesForEnvironment(UUID eid);

  List<FeatureValue> updateAllFeatureValuesForEnvironment(UUID eid, List<FeatureValue> featureValues,
                                                          PersonFeaturePermission requireRoleCheck) throws OptimisticLockingException, NoAppropriateRole, RolloutStrategyValidator.InvalidStrategyCombination;

  List<FeatureEnvironment> getFeatureValuesForApplicationForKeyForPerson(UUID appId, String key, Person person);
}
