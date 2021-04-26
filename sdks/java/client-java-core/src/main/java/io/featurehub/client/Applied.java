package io.featurehub.client;

public class Applied {
  private final boolean matched;
  private final Object value;

  public Applied(boolean matched, Object value) {
    this.matched = matched;
    this.value = value;
  }

  public boolean isMatched() {
    return matched;
  }

  public Object getValue() {
    return value;
  }
}
