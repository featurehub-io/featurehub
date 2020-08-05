package io.featurehub.server.jersey;

import io.featurehub.client.FeatureRepository;
import org.glassfish.jersey.server.monitoring.ApplicationEvent;
import org.glassfish.jersey.server.monitoring.ApplicationEventListener;
import org.glassfish.jersey.server.monitoring.RequestEvent;
import org.glassfish.jersey.server.monitoring.RequestEventListener;
import javax.ws.rs.core.Response;
import javax.inject.Inject;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class FeatureRequiredApplicationEventListener implements ApplicationEventListener {
  private final FeatureRepository featureRepository;

  @Inject
  public FeatureRequiredApplicationEventListener(FeatureRepository featureRepository) {
    this.featureRepository = featureRepository;
  }

  @Override
  public void onEvent(ApplicationEvent event) {
  }

  @Override
  public RequestEventListener onRequest(RequestEvent requestEvent) {
    return new FeatureRequiredEvent(featureRepository);
  }

  static class FeatureInfo {
    public final String[] features;

    FeatureInfo(String[] features) {
      this.features = features;
    }
  }

  static Map<Method, FeatureInfo> featureInfo = new ConcurrentHashMap<>();

  static class FeatureRequiredEvent implements RequestEventListener {
    private final FeatureRepository featureRepository;

    FeatureRequiredEvent(FeatureRepository featureRepository) {
      this.featureRepository = featureRepository;
    }

    @Override
    public void onEvent(RequestEvent event) {
      if (event.getType() == RequestEvent.Type.REQUEST_MATCHED) {
        featureCheck(event);
      }
    }

    private void featureCheck(RequestEvent event) {
      FeatureInfo fi = featureInfo.computeIfAbsent(getMethod(event), this::extractFeatureInfo);

      // if any of the flags mentioned are OFF, return NOT_FOUND
      if (fi.features.length > 0) {
        for(String feature : fi.features) {
          if (Boolean.FALSE.equals(featureRepository.getFeatureState(feature).getBoolean())) {
            event.getContainerRequest().abortWith(Response.status(Response.Status.NOT_FOUND).build());
            return;
          }
        }
      }
    }

    private FeatureInfo extractFeatureInfo(Method m) {
      FeatureFlagEnabled fr = m.getDeclaredAnnotation(FeatureFlagEnabled.class);

      if (fr == null) {
        fr = m.getDeclaringClass().getAnnotation(FeatureFlagEnabled.class);

        if (fr == null) {
          for (Class<?> anInterface : m.getDeclaringClass().getInterfaces()) {
            fr = anInterface.getAnnotation(FeatureFlagEnabled.class);
            if (fr != null) {
              break;
            }
          }
        }
      }

      if (fr != null) {
        return new FeatureInfo(fr.features());
      }

      return NO_FEATURES_REQUIRED;
    }

    private static final FeatureInfo NO_FEATURES_REQUIRED = new FeatureInfo(new String[]{});

    Method getMethod(RequestEvent event) {
      return event.getUriInfo().getMatchedResourceMethod().getInvocable().getHandlingMethod();
    }
  }
}
