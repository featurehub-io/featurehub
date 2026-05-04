package io.featurehub.jersey.config;

import cd.connect.openapi.support.OpenApiEnumProvider;
import io.featurehub.jersey.OffsetDateTimeQueryProvider;
import io.featurehub.jersey.SSEAwareEncodingFilter;
import io.featurehub.lifecycle.LifecycleListenerFeature;
import io.featurehub.rest.WebHeaderAuditLogger;
import io.featurehub.utils.FallbackPropertyConfig;
import jakarta.ws.rs.core.Feature;
import jakarta.ws.rs.core.FeatureContext;
import org.glassfish.jersey.CommonProperties;
import org.glassfish.jersey.jackson.internal.jackson.jaxrs.json.JacksonJaxbJsonProvider;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.message.GZipEncoder;

/**
 * This class is used in clients and servers, so only classes that are relevant to
 * both should be registered here.
 *
 * It is registered in CommonFeatureHubFeatures, which is in turn registered in FeatureHubJerseyHost.
 */
public class CommonConfiguration implements Feature {

  @Override
  public boolean configure(FeatureContext config) {
    config.property(CommonProperties.METAINF_SERVICES_LOOKUP_DISABLE, true);
    config.property(CommonProperties.FEATURE_AUTO_DISCOVERY_DISABLE, true);
    config.property(CommonProperties.MOXY_JSON_FEATURE_DISABLE, true);

    if (!"true".equals(FallbackPropertyConfig.Companion.getConfig("http-compression-disable", "false"))) {
      config.register(SSEAwareEncodingFilter.class);
      config.register(GZipEncoder.class);
    }

    // this is the objectmapper we want to use
    config.register(JacksonContextProvider.class);
    // this forces all requests to use an objectmapper to use our application wide singleton
    config.register(new JacksonJaxbJsonProvider(JacksonObjectProvider.mapper, JacksonJaxbJsonProvider.DEFAULT_ANNOTATIONS));
    config.register(MultiPartFeature.class);
    config.register(GZipEncoder.class);
    config.register(LocalExceptionMapper.class);
    config.register(OffsetDateTimeQueryProvider.class);
    config.register(OpenApiEnumProvider.class);
    config.register(LifecycleListenerFeature.class);

    // only wire this up if the config is actually there
    if (FallbackPropertyConfig.Companion.getConfig(WebHeaderAuditLogger.Companion.getCONFIG_KEY())
        != null) {
      config.register(WebHeaderAuditLogger.class);
    }

    return true;
  }
}
