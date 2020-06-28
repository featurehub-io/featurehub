package io.featurehub.db.listener;

import io.featurehub.mr.model.FeatureValue;
import io.featurehub.mr.model.FeatureValueType;

import java.util.function.Function;

public interface FeatureUpdateBySDKApi {
  void updateFeature(String sdkUrl, String envId, String featureKey, boolean updatingValue, Function<FeatureValueType, FeatureValue> buildFeatureValue);
}
