package io.featurehub.db.api;

import io.featurehub.mr.model.ApplicationFeatureValues;
import io.featurehub.mr.model.EnvironmentFeaturesResult;
import io.featurehub.mr.model.FeatureEnvironment;
import io.featurehub.mr.model.FeatureValue;
import io.featurehub.mr.model.Person;

import java.util.List;

public interface FeatureApi {

  void updateAllFeatureValuesByApplicationForKey(String id, String key, List<FeatureValue> featureValue, Person from, boolean removeValuesNotPassed) throws OptimisticLockingException, NoAppropriateRole;

  ApplicationFeatureValues findAllFeatureAndFeatureValuesForEnvironmentsByApplication(String appId, Person current);

  EnvironmentFeaturesResult lastFeatureValueChanges(Person from);

  class NoAppropriateRole extends Exception {
  }

  FeatureValue createFeatureValueForEnvironment(String eid, String key, FeatureValue featureValue, PersonFeaturePermission person) throws OptimisticLockingException, NoAppropriateRole;

  boolean deleteFeatureValueForEnvironment(String eid, String key);

  FeatureValue updateFeatureValueForEnvironment(String eid, String key, FeatureValue featureValue, PersonFeaturePermission person) throws OptimisticLockingException, NoAppropriateRole;

  FeatureValue getFeatureValueForEnvironment(String eid, String key);

  EnvironmentFeaturesResult getAllFeatureValuesForEnvironment(String eid);

  List<FeatureValue> updateAllFeatureValuesForEnvironment(String eid, List<FeatureValue> featureValues, PersonFeaturePermission requireRoleCheck) throws OptimisticLockingException, NoAppropriateRole;

  List<FeatureEnvironment> getFeatureValuesForApplicationForKeyForPerson(String appId, String key, Person person);
}
