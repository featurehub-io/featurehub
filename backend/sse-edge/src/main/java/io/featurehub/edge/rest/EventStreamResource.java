package io.featurehub.edge.rest;

import io.featurehub.edge.FeatureTransformer;
import io.featurehub.edge.FeatureTransformerUtils;
import io.featurehub.edge.ServerConfig;
import io.featurehub.edge.bucket.EventOutputBucketService;
import io.featurehub.edge.client.ClientConnection;
import io.featurehub.edge.client.TimedBucketClientConnection;
import io.featurehub.mr.messaging.StreamedFeatureUpdate;
import io.featurehub.mr.model.EdgeInitPermissionResponse;
import io.featurehub.mr.model.FeatureValue;
import io.featurehub.mr.model.RoleType;
import io.featurehub.sse.model.Environment;
import io.featurehub.sse.model.FeatureStateUpdate;
import org.glassfish.jersey.media.sse.EventOutput;
import org.glassfish.jersey.media.sse.SseFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.GET;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import java.math.BigDecimal;
import java.util.List;

@Path("/features")
public class EventStreamResource {
  private static final Logger log = LoggerFactory.getLogger(EventStreamResource.class);

  private final EventOutputBucketService bucketService;
  private final ServerConfig serverConfig;
  private final FeatureTransformer featureTransformer;

  @Inject
  public EventStreamResource(EventOutputBucketService bucketService, ServerConfig serverConfig) {
    this.bucketService = bucketService;
    this.serverConfig = serverConfig;
    featureTransformer = new FeatureTransformerUtils();
  }

  @GET
  @Path("/")
  @Produces({ "application/json" })
  public List<Environment> getFeatureStates(@QueryParam("sdkUrl") List<String> sdkUrl) {
    if (sdkUrl == null || sdkUrl.isEmpty()) {
      throw new BadRequestException();
    }
    return serverConfig.requestFeatures(sdkUrl);
  }


  @GET
  @Path("{namedCache}/{environmentId}/{apiKey}")
  @Produces(SseFeature.SERVER_SENT_EVENTS)
  public EventOutput features(@PathParam("namedCache") String namedCache,
                              @PathParam("environmentId") String envId,
                              @PathParam("apiKey") String apiKey, FeatureStateUpdate update) {
    EventOutput o = new EventOutput();

    try {
      ClientConnection b = new TimedBucketClientConnection.Builder()
        .featureTransformer(featureTransformer)
        .environmentId(envId).apiKey(apiKey).namedCache(namedCache).output(o).build();

      if (b.discovery()) {
        serverConfig.requestFeatures(b);

        bucketService.putInBucket(b);
      }
    } catch (Exception e) {
      log.error("failed to write feature states");
      throw new InternalServerErrorException(e);
    }

    return o;
  }

  /**
   * We do a double check of all permissions and values at Edge to ensure that as much load as possible
   * is kept off MR. MR will do these checks against this set of permissions again, but updates are done via
   * NATs and not via REST.
   */
  @PUT
  @Path("{namedCache}/{environmentId}/{apiKey}/{featureKey}")
  public Response update(@PathParam("namedCache") String namedCache,
                         @PathParam("environmentId") String envId,
                         @PathParam("apiKey") String apiKey,
                         @PathParam("featureKey") String featureKey,
                         FeatureStateUpdate featureStateUpdate) {

    final EdgeInitPermissionResponse perms = serverConfig.requestPermission(namedCache, apiKey, envId, featureKey);

    if (perms == null || Boolean.FALSE.equals(perms.getSuccess())) {
      return Response.status(Response.Status.NOT_FOUND).build();
    }

    if (perms.getRoles().isEmpty() || (perms.getRoles().size() == 1 && perms.getRoles().get(0) == RoleType.READ)) {
      return Response.status(Response.Status.FORBIDDEN).build();
    }

    if (Boolean.TRUE.equals(featureStateUpdate.getLock())) {
      if (!perms.getRoles().contains(RoleType.LOCK)) {
        return Response.status(Response.Status.FORBIDDEN).build();
      }
    } else if (Boolean.FALSE.equals(featureStateUpdate.getLock())) {
      if (!perms.getRoles().contains(RoleType.UNLOCK)) {
        return Response.status(Response.Status.FORBIDDEN).build();
      }
    }

    if (featureStateUpdate.getValue() != null) {
      featureStateUpdate.setUpdateValue(Boolean.TRUE);
    }

    // nothing to do?
    if (featureStateUpdate.getLock() == null && (featureStateUpdate.getUpdateValue() == null || Boolean.FALSE.equals(featureStateUpdate.getUpdateValue()) )) {
      return Response.status(Response.Status.BAD_REQUEST).build();
    }

    if (Boolean.TRUE.equals(featureStateUpdate.getUpdateValue())) {
      if (!perms.getRoles().contains(RoleType.CHANGE_VALUE)) {
        return Response.status(Response.Status.FORBIDDEN).build();
      } else if (Boolean.TRUE.equals(perms.getFeature().getValue().getLocked()) && !Boolean.FALSE.equals(featureStateUpdate.getLock())) {
        // its locked and you are trying to change its value and not unlocking it at the same time, that makes no sense
        return Response.status(Response.Status.PRECONDITION_FAILED).build();
      }
    }

    final StreamedFeatureUpdate upd = new StreamedFeatureUpdate()
      .apiKey(apiKey)
      .environmentId(envId)
      .updatingValue(featureStateUpdate.getUpdateValue())
      .lock(featureStateUpdate.getLock())
      .featureKey(featureKey);

    // now update our internal value we will be sending, and also check
    // if aren't actually changing anything
    final FeatureValue value = perms.getFeature().getValue();
    boolean lockChanging = upd.getLock() != null && !upd.getLock().equals(value.getLocked());
    boolean valueNotActuallyChanging = false;
    if (Boolean.TRUE.equals(featureStateUpdate.getUpdateValue())) {

      if (featureStateUpdate.getValue() != null) {
        final String val = featureStateUpdate.getValue().toString();

        switch (perms.getFeature().getFeature().getValueType()) {
          case BOOLEAN:
            // if it is true, TRUE, t its true.
            upd.valueBoolean(val.toLowerCase().startsWith("t"));
            valueNotActuallyChanging = upd.getValueBoolean().equals(value.getValueBoolean());
            break;
          case STRING:
            upd.valueString(val);
            valueNotActuallyChanging = upd.getValueString().equals(value.getValueString());
            break;
          case JSON:
            // TODO: implement JSON Schema for JSON data and validate it here
            valueNotActuallyChanging = upd.getValueString().equals(value.getValueJson());
            break;
          case NUMBER:
            try {
              upd.valueNumber(new BigDecimal(val));
              valueNotActuallyChanging = upd.getValueNumber().equals(value.getValueNumber());
            } catch (Exception e) {
              return Response.status(Response.Status.BAD_REQUEST).build();
            }
            break;
        }
      } else {
        switch (perms.getFeature().getFeature().getValueType()) {
          case BOOLEAN:
            // a null boolean is not valid
            return Response.status(Response.Status.PRECONDITION_FAILED).build();
          case STRING:
            valueNotActuallyChanging = (value.getValueString() == null);
            break;
          case NUMBER:
            valueNotActuallyChanging = (value.getValueNumber() == null);
            break;
          case JSON:
            valueNotActuallyChanging = (value.getValueJson() == null);
            break;
        }
      }
    }

    if (valueNotActuallyChanging && !lockChanging) {
      return Response.status(Response.Status.ACCEPTED).build();
    }

    if (valueNotActuallyChanging) {
      upd.updatingValue(null);
      upd.valueBoolean(null);
      upd.valueNumber(null);
      upd.valueString(null);
    }

    log.debug("publishing update on {} for {}", namedCache, upd);
    serverConfig.publishFeatureChangeRequest(upd, namedCache);

    return Response.ok().build();
  }
}
