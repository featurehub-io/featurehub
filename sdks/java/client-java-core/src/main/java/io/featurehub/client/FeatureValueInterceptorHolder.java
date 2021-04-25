package io.featurehub.client;

public class FeatureValueInterceptorHolder {
  public final boolean allowLockOverride;
  public final FeatureValueInterceptor interceptor;

  public FeatureValueInterceptorHolder(boolean allowLockOverride, FeatureValueInterceptor interceptor) {
    this.allowLockOverride = allowLockOverride;
    this.interceptor = interceptor;
  }
}
