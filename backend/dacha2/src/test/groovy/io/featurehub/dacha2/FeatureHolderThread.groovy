package io.featurehub.dacha2

import io.featurehub.dacha.model.CacheEnvironmentFeature

import java.util.concurrent.CompletableFuture

class FeatureHolderThread extends Thread {
  public final EnvironmentFeatures env;
  public final CacheEnvironmentFeature feature;
  public final CompletableFuture<Boolean> future;

  FeatureHolderThread(EnvironmentFeatures env, CacheEnvironmentFeature feature) {
    this.env = env;
    this.feature = feature;
    this.future = new CompletableFuture<>();
  }

  @Override
  void run() {
    10.times {
      env.setFeature(feature)
    }
    future.complete(true)
  }
}

