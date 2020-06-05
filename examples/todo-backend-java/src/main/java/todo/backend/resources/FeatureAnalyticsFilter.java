package todo.backend.resources;

import io.featurehub.client.GoogleAnalyticsApiClient;
import io.featurehub.client.StaticFeatureContext;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FeatureAnalyticsFilter implements ContainerRequestFilter, ContainerResponseFilter {
  @Override
  public void filter(ContainerRequestContext requestContext) throws IOException {
    requestContext.setProperty("startTime", System.currentTimeMillis());
  }

  @Override
  public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {
    Long start = (Long)requestContext.getProperty("startTime");
    long duration = 0;
    if (start != null) {
      duration = System.currentTimeMillis() - start;
    }

    Map<String, String> other = new HashMap<>();
    other.put(GoogleAnalyticsApiClient.GA_VALUE, Long.toString(duration));
    final List<String> matchedURIs = requestContext.getUriInfo().getMatchedURIs();
    if (matchedURIs.size() > 0) {
      StaticFeatureContext.getInstance().logAnalyticsEvent(matchedURIs.get(0), other);
    }

  }
}
