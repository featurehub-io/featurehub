package io.featurehub.db.publish;

import io.featurehub.dacha.model.PublishEnvironment;
import io.featurehub.dacha.model.PublishFeatureValue;
import io.featurehub.dacha.model.PublishServiceAccount;

public interface CacheBroadcast {
  void publishEnvironment(PublishEnvironment eci);

  void publishServiceAccount(PublishServiceAccount saci);

  void publishFeature(PublishFeatureValue feature);
}
