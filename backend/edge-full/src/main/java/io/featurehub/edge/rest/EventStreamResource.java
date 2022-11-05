package io.featurehub.edge.rest;

import cd.connect.jersey.prometheus.Prometheus;
import io.featurehub.edge.stats.StatRecorder;
import io.featurehub.edge.utils.FastlyResponseWrapper;
import io.featurehub.sse.model.FeatureStateUpdate;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.container.AsyncResponse;
import jakarta.ws.rs.container.Suspended;
import jakarta.ws.rs.core.Response;
import org.glassfish.hk2.api.Immediate;
import org.glassfish.jersey.media.sse.EventOutput;
import org.glassfish.jersey.media.sse.SseFeature;
import org.glassfish.jersey.server.ManagedAsync;

import java.util.List;
import java.util.UUID;

@Path("/features")
@Immediate
public class EventStreamResource {
  private final StatRecorder statRecorder;
  private final FeatureSse featureSse;
  private final FeatureGet featureGetProcessor;
  private final FeatureUpdate featureUpdateProcessor;
  private final boolean fastlyConfigured;

  @Inject
  public EventStreamResource(
      StatRecorder statRecorder,
      FeatureSse featureSse,
      FeatureGet featureGetProcessor,
      FeatureUpdate featureUpdateProcessor) {
    this.statRecorder = statRecorder;
    this.featureSse = featureSse;
    this.featureGetProcessor = featureGetProcessor;
    this.featureUpdateProcessor = featureUpdateProcessor;
    this.fastlyConfigured = FastlyResponseWrapper.Companion.fastlyConfigured();
  }

  // support new and old style for GET - apiKeys and sdkUrl, they are the same we just want to
  // transition the
  // naming at some point
  @GET
  @Path("/")
  @Produces({"application/json"})
  @Prometheus(name = "edge_poll_api", help = "Number of requests for the poll API")
  @ManagedAsync
  public void getFeatureStates(
      @Suspended AsyncResponse response,
      @QueryParam("sdkUrl") List<String> sdkUrls,
      @QueryParam("apiKey") List<String> apiKeys,
      @QueryParam("contextSha") String contextSha,
      @HeaderParam("x-featurehub") List<String> featureHubAttrs,
      @HeaderParam("if-none-match") String etagHeader) {
    if (fastlyConfigured && contextSha == null) {
      throw new WebApplicationException(Response.status(400).header("content-type", "text/plain").entity(
        "Fastly is configured and your SDK is too old to support it and will " +
        "malfunction, please update.").build());
    }

    featureGetProcessor.processGet(
        response, sdkUrls, apiKeys, featureHubAttrs, etagHeader, statRecorder);
  }

  @GET
  @Path("{namedCache}/{environmentId}/{apiKey}")
  @Prometheus(name = "edge_sse_api", help = "Number of requests for the SSE API")
  @Produces(SseFeature.SERVER_SENT_EVENTS)
  public EventOutput features(
      @PathParam("namedCache") String namedCache,
      @PathParam("environmentId") UUID envId,
      @PathParam("apiKey") String apiKey,
      @HeaderParam("x-featurehub") List<String> featureHubAttrs, // non browsers can set headers
      @HeaderParam("x-fh-extraconfig") String extraConfig,
      @QueryParam("xfeaturehub") String browserHubAttrs, // browsers can't set headers,
      @HeaderParam("Last-Event-ID") String etag) {
    return featureSse.process(namedCache, envId, apiKey, featureHubAttrs, browserHubAttrs, etag, extraConfig);
  }

  @GET
  @Path("{environmentId}/{apiKey}")
  @Prometheus(name = "edge_sse_api", help = "Number of requests for the SSE API")
  @Produces(SseFeature.SERVER_SENT_EVENTS)
  public EventOutput features(
    @PathParam("environmentId") UUID envId,
    @PathParam("apiKey") String apiKey,
    @HeaderParam("x-featurehub") List<String> featureHubAttrs, // non browsers can set headers
    @HeaderParam("x-fh-extraconfig") String extraConfig,
    @QueryParam("xfeaturehub") String browserHubAttrs, // browsers can't set headers,
    @HeaderParam("Last-Event-ID") String etag) {
    return featureSse.process(null, envId, apiKey, featureHubAttrs, browserHubAttrs, etag, extraConfig);
  }

  /**
   * We do a double check of all permissions and values at Edge to ensure that as much load as
   * possible is kept off MR. MR will do these checks against this set of permissions again, but
   * updates are done via NATs and not via REST.
   */
  @PUT
  @Path("{namedCache}/{environmentId}/{apiKey}/{featureKey}")
  @Prometheus(name = "edge_test_sdk_api", help = "Number of requests to the test SDK API")
  @ManagedAsync
  public void update(
      @Suspended AsyncResponse response,
      @PathParam("namedCache") String namedCache,
      @PathParam("environmentId") UUID envId,
      @PathParam("apiKey") String apiKey,
      @PathParam("featureKey") String featureKey,
      FeatureStateUpdate featureStateUpdate) {
    featureUpdateProcessor.updateFeature(
        response, namedCache, envId, apiKey, featureKey, featureStateUpdate, statRecorder);
  }

  @PUT
  @Path("{environmentId}/{apiKey}/{featureKey}")
  @Prometheus(name = "edge_test_sdk_api", help = "Number of requests to the test SDK API")
  @ManagedAsync
  public void update(
      @Suspended AsyncResponse response,
      @PathParam("environmentId") UUID envId,
      @PathParam("apiKey") String apiKey,
      @PathParam("featureKey") String featureKey,
      FeatureStateUpdate featureStateUpdate) {
    featureUpdateProcessor.updateFeature(
        response, null, envId, apiKey, featureKey, featureStateUpdate, statRecorder);
  }
}
