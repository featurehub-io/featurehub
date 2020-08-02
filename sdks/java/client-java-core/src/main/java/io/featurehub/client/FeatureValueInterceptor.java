package io.featurehub.client;

/**
 * Value interceptors allow us to register alternative ways to get features into the system. Even when features
 * are otherwise unrecognized (for example, they are stored against a key, but they have no feature state, so
 * they have not come via the usual route of loading the features in via a repository).
 * <p>
 * By their very nature they are contextual so they never trigger events, they can only be used imperatively. As such
 * they are designed to reflect changes to _local_ state, state local to a method call.
 */
public interface FeatureValueInterceptor {
  /**
   * get the value associated with this key (if any)
   *
   * @param key - the key we are looking for
   * @return - a ValueMatch object indicating whether we matched and if so, what is the value
   */
  ValueMatch getValue(String key);

  class ValueMatch {
    public final boolean matched;
    public final String value;

    public ValueMatch(boolean matched, String value) {
      this.matched = matched;
      this.value = value;
    }
  }
}
