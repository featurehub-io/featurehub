package io.featurehub.dacha.api;

import cd.connect.app.config.DeclaredConfigResolver;
import jakarta.inject.Singleton;
import jakarta.ws.rs.core.Feature;
import jakarta.ws.rs.core.FeatureContext;
import org.glassfish.jersey.internal.inject.AbstractBinder;

public class DachaClientFeature implements Feature  {

  public DachaClientFeature() {
    DeclaredConfigResolver.resolve(this);
  }

  @Override
  public boolean configure(FeatureContext featureContext) {
    featureContext.register(new AbstractBinder() {
      @Override
      protected void configure() {
        bind(DachaClientRegistry.class).to(DachaClientServiceRegistry.class).in(Singleton.class);
      }
    });

    return true;
  }
}
