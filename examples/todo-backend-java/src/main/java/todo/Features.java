package todo;

import io.featurehub.client.Feature;
import io.featurehub.client.StaticFeatureContext;

public enum Features implements Feature {

  FEATURE_TITLE_TO_UPPERCASE;

  private Features() {
  }

  public boolean isActive() {
    return StaticFeatureContext.getInstance().isActive(this);
  }
}
