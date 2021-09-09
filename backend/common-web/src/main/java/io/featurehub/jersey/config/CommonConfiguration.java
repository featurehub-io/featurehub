package io.featurehub.jersey.config;

import cd.connect.openapi.support.OpenApiEnumProvider;
import io.featurehub.jersey.OffsetDateTimeQueryProvider;
import io.featurehub.utils.CurrentTime;
import io.featurehub.utils.CurrentTimeSource;
import jakarta.inject.Singleton;
import jakarta.ws.rs.core.Configurable;
import jakarta.ws.rs.core.Feature;
import jakarta.ws.rs.core.FeatureContext;
import org.glassfish.jersey.CommonProperties;
import org.glassfish.jersey.internal.inject.AbstractBinder;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.message.GZipEncoder;

public class CommonConfiguration implements Feature {

  public static void basic(Configurable<? extends Configurable> config) {
    config.property(CommonProperties.METAINF_SERVICES_LOOKUP_DISABLE, true);
    config.property(CommonProperties.FEATURE_AUTO_DISCOVERY_DISABLE, true);
    config.property(CommonProperties.MOXY_JSON_FEATURE_DISABLE, true);

    config.register(JacksonFeature.class);
    config.register(MultiPartFeature.class);
    config.register(GZipEncoder.class);
    config.register(JacksonContextProvider.class);
    config.register(LocalExceptionMapper.class);
    config.register(OffsetDateTimeQueryProvider.class);
    config.register(OpenApiEnumProvider.class);
  }

  @Override
  public boolean configure(FeatureContext featureContext) {
    basic(featureContext);

    featureContext.register(new AbstractBinder() {
      @Override
      protected void configure() {
        bind(CurrentTime.class).to(CurrentTimeSource.class).in(Singleton.class);
      }
    });
    return true;
  }
}
