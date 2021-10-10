package io.featurehub.edge.rest;

import io.featurehub.edge.stats.StatRecorder;
import io.featurehub.sse.model.FeatureStateUpdate;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.container.AsyncResponse;
import jakarta.ws.rs.container.Suspended;
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
  }

  // support new and old style for GET - apiKeys and sdkUrl, they are the same we just want to
  // transition the
  // naming at some point
  @GET
  @Path("/")
  @Produces({"application/json"})
  @ManagedAsync
  public void getFeatureStates(
      @Suspended AsyncResponse response,
      @QueryParam("sdkUrl") List<String> sdkUrls,
      @QueryParam("apiKey") List<String> apiKeys,
      @HeaderParam("x-featurehub") List<String> featureHubAttrs,
      @HeaderParam("if-none-match") String etagHeader) {

    featureGetProcessor.processGet(
        response, sdkUrls, apiKeys, featureHubAttrs, etagHeader, statRecorder);
  }

  @GET
  @Path("{namedCache}/{environmentId}/{apiKey}")
  @Produces(SseFeature.SERVER_SENT_EVENTS)
  public EventOutput features(
      @PathParam("namedCache") String namedCache,
      @PathParam("environmentId") UUID envId,
      @PathParam("apiKey") String apiKey,
      @HeaderParam("x-featurehub") List<String> featureHubAttrs, // non browsers can set headers
      @QueryParam("xfeaturehub") String browserHubAttrs, // browsers can't set headers,
      @HeaderParam("Last-Event-ID") String etag) {
    return featureSse.process(namedCache, envId, apiKey, featureHubAttrs, browserHubAttrs, etag);
  }

  /**
   * We do a double check of all permissions and values at Edge to ensure that as much load as
   * possible is kept off MR. MR will do these checks against this set of permissions again, but
   * updates are done via NATs and not via REST.
   */
  @PUT
  @Path("{namedCache}/{environmentId}/{apiKey}/{featureKey}")
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
}
