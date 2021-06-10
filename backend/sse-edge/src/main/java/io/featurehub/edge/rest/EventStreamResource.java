package io.featurehub.edge.rest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.featurehub.edge.FeatureTransformer;
import io.featurehub.edge.FeatureTransformerUtils;
import io.featurehub.edge.ServerConfig;
import io.featurehub.edge.bucket.EventOutputBucketService;
import io.featurehub.edge.client.ClientConnection;
import io.featurehub.edge.client.TimedBucketClientConnection;
import io.featurehub.edge.KeyParts;
import io.featurehub.edge.stats.StatRecorder;
import io.featurehub.edge.strategies.ClientContext;
import io.featurehub.mr.messaging.StreamedFeatureUpdate;
import io.featurehub.mr.model.EdgeInitPermissionResponse;
import io.featurehub.mr.model.FeatureValue;
import io.featurehub.mr.model.RoleType;
import io.featurehub.sse.model.Environment;
import io.featurehub.sse.model.FeatureStateUpdate;
import io.featurehub.sse.stats.model.EdgeHitResultType;
import io.featurehub.sse.stats.model.EdgeHitSourceType;
import io.prometheus.client.Histogram;
import org.glassfish.jersey.media.sse.EventOutput;
import org.glassfish.jersey.media.sse.SseFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Path("/features")
public class EventStreamResource {
  private static final Logger log = LoggerFactory.getLogger(EventStreamResource.class);

  private final EventOutputBucketService bucketService;
  private final ServerConfig serverConfig;
  private final StatRecorder statRecorder;
  private final FeatureTransformer featureTransformer;
  private final ObjectMapper mapper = new ObjectMapper();

  // we are doing timers here rather than instrumenting Jersey because in this case the names are more interesting and
  // useful in the sea of metrics
  private static final Histogram pollSpeedHistogram = Histogram.build("edge_conn_length_poll", "The length of " +
    "time that the connection is open for Polling clients").create();
  private static final Histogram testSpeedHistogram = Histogram.build("edge_conn_length_test", "The length of " +
    "time that the connection is open for Testing clients").create();

  @Inject
  public EventStreamResource(EventOutputBucketService bucketService, ServerConfig serverConfig, StatRecorder statRecorder) {
    this.bucketService = bucketService;
    this.serverConfig = serverConfig;
    this.statRecorder = statRecorder;
    featureTransformer = new FeatureTransformerUtils();

    mapper.enable(DeserializationFeature.FAIL_ON_READING_DUP_TREE_KEY);
  }

  // support new and old style for GET - apiKeys and sdkUrl, they are the same we just want to transition the
  // naming at some point
  @GET
  @Path("/")
  @Produces({ "application/json" })
  public List<Environment> getFeatureStates(@QueryParam("sdkUrl") List<String> sdkUrls,
                                            @QueryParam("apiKeys") List<String> apiKeys,
                                            @HeaderParam("x-featurehub") List<String> featureHubAttrs) {
    if ((sdkUrls == null || sdkUrls.isEmpty()) && (apiKeys == null || apiKeys.isEmpty()) ) {
      throw new BadRequestException();
    }

    final Histogram.Timer timer = pollSpeedHistogram.startTimer();

    final List<KeyParts> realApiKeys =
      (sdkUrls == null || sdkUrls.isEmpty() ? apiKeys : sdkUrls)
        .stream()
        .distinct() // we want unique ones
        .map(KeyParts.Companion::fromString)
        .filter(Objects::nonNull).collect(Collectors.toList());

    final List<Environment> environments = serverConfig.requestFeatures(realApiKeys,
      ClientContext.decode(featureHubAttrs, realApiKeys));

    // record the result
    realApiKeys.forEach(k -> {
      statRecorder.recordHit(k,
        environments.stream().anyMatch(e -> e.getId().equals(k.getEnvironmentId())) ?
          EdgeHitResultType.SUCCESS : EdgeHitResultType.MISSED, EdgeHitSourceType.POLL );
    });

    timer.observeDuration();

    if (environments.isEmpty()) {
      throw new NotFoundException(); // no environments were found
    }

    return environments;
  }

  @GET
  @Path("{namedCache}/{environmentId}/{apiKey}")
  @Produces(SseFeature.SERVER_SENT_EVENTS)
  public EventOutput features(@PathParam("namedCache") String namedCache,
                              @PathParam("environmentId") UUID envId,
                              @PathParam("apiKey") String apiKey,
                              @HeaderParam("x-featurehub") List<String> featureHubAttrs, // non browsers can set headers
                              @QueryParam("xfeaturehub") String browserHubAttrs // browsers can't set headers
                              ) {
    EventOutput o = new EventOutput();

    final KeyParts key = new KeyParts(namedCache, envId, apiKey);

    try {
      ClientConnection b = new TimedBucketClientConnection.Builder()
        .featureTransformer(featureTransformer)
        .statRecorder(statRecorder)
        .apiKey(key)
        .featureHubAttributes(browserHubAttrs == null ? featureHubAttrs : Collections.singletonList(browserHubAttrs))
        .output(o)
        .build();

      if (b.discovery()) {
        serverConfig.requestFeatures(b);

        bucketService.putInBucket(b);
      } else {
        statRecorder.recordHit(key, EdgeHitResultType.FAILED_TO_WRITE_ON_INIT, EdgeHitSourceType.EVENTSOURCE);
      }
    } catch (Exception e) {
      statRecorder.recordHit(key, EdgeHitResultType.FAILED_TO_PROCESS_REQUEST, EdgeHitSourceType.EVENTSOURCE);
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
                         @PathParam("environmentId") UUID envId,
                         @PathParam("apiKey") String apiKey,
                         @PathParam("featureKey") String featureKey,
                         FeatureStateUpdate featureStateUpdate) {

    Histogram.Timer timer = testSpeedHistogram.startTimer();

    try {
      return testAPi(namedCache, envId, apiKey, featureKey, featureStateUpdate);
    } finally{
      timer.observeDuration();
    }
  }

  private Response testAPi(String namedCache, UUID envId, String apiKey, String featureKey,
                           FeatureStateUpdate featureStateUpdate) {
    final KeyParts key = new KeyParts(namedCache, envId, apiKey);

    try {
      final EdgeInitPermissionResponse perms = serverConfig.requestPermission(namedCache, apiKey, envId, featureKey);

      if (perms == null || Boolean.FALSE.equals(perms.getSuccess())) {
        statRecorder.recordHit(key, EdgeHitResultType.MISSED, EdgeHitSourceType.TESTSDK);
        return Response.status(Response.Status.NOT_FOUND).build();
      }

      if (perms.getRoles().isEmpty() || (perms.getRoles().size() == 1 && perms.getRoles().get(0) == RoleType.READ)) {
        statRecorder.recordHit(key, EdgeHitResultType.FORBIDDEN, EdgeHitSourceType.TESTSDK);
        return Response.status(Response.Status.FORBIDDEN).build();
      }

      if (Boolean.TRUE.equals(featureStateUpdate.getLock())) {
        if (!perms.getRoles().contains(RoleType.LOCK)) {
          statRecorder.recordHit(key, EdgeHitResultType.FORBIDDEN, EdgeHitSourceType.TESTSDK);
          return Response.status(Response.Status.FORBIDDEN).build();
        }
      } else if (Boolean.FALSE.equals(featureStateUpdate.getLock())) {
        if (!perms.getRoles().contains(RoleType.UNLOCK)) {
          statRecorder.recordHit(key, EdgeHitResultType.FORBIDDEN, EdgeHitSourceType.TESTSDK);
          return Response.status(Response.Status.FORBIDDEN).build();
        }
      }

      if (featureStateUpdate.getValue() != null) {
        featureStateUpdate.setUpdateValue(Boolean.TRUE);
      }

      // nothing to do?
      if (featureStateUpdate.getLock() == null && (featureStateUpdate.getUpdateValue() == null || Boolean.FALSE.equals(featureStateUpdate.getUpdateValue()) )) {
        statRecorder.recordHit(key, EdgeHitResultType.FAILED_TO_PROCESS_REQUEST, EdgeHitSourceType.TESTSDK);
        return Response.status(Response.Status.BAD_REQUEST).build();
      }

      if (Boolean.TRUE.equals(featureStateUpdate.getUpdateValue())) {
        if (!perms.getRoles().contains(RoleType.CHANGE_VALUE)) {
          statRecorder.recordHit(key, EdgeHitResultType.FORBIDDEN, EdgeHitSourceType.TESTSDK);
          return Response.status(Response.Status.FORBIDDEN).build();
        } else if (Boolean.TRUE.equals(perms.getFeature().getValue().getLocked()) && !Boolean.FALSE.equals(featureStateUpdate.getLock())) {
          // its locked and you are trying to change its value and not unlocking it at the same time, that makes no sense
          statRecorder.recordHit(key, EdgeHitResultType.UPDATE_NONSENSE, EdgeHitSourceType.TESTSDK);
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
              // must be true or false in some case
              if (!val.equalsIgnoreCase("true") && !val.equalsIgnoreCase("false")) {
                statRecorder.recordHit(key, EdgeHitResultType.FAILED_TO_PROCESS_REQUEST, EdgeHitSourceType.TESTSDK);
                return Response.status(Response.Status.BAD_REQUEST).build();
              }

              upd.valueBoolean(Boolean.parseBoolean(val));

              valueNotActuallyChanging = upd.getValueBoolean().equals(value.getValueBoolean());
              break;
            case STRING:
              upd.valueString(val);
              valueNotActuallyChanging = upd.getValueString().equals(value.getValueString());
              break;
            case JSON:
              try {
                mapper.readTree(val);
              } catch (JsonProcessingException jpe) {
                statRecorder.recordHit(key, EdgeHitResultType.FAILED_TO_PROCESS_REQUEST, EdgeHitSourceType.TESTSDK);
                return Response.status(Response.Status.BAD_REQUEST).build();
              }

              upd.valueString(val);
              valueNotActuallyChanging = upd.getValueString().equals(value.getValueJson());
              break;
            case NUMBER:
              try {
                upd.valueNumber(new BigDecimal(val));
                valueNotActuallyChanging = upd.getValueNumber().equals(value.getValueNumber());
              } catch (Exception e) {
                statRecorder.recordHit(key, EdgeHitResultType.FAILED_TO_PROCESS_REQUEST, EdgeHitSourceType.TESTSDK);
                return Response.status(Response.Status.BAD_REQUEST).build();
              }
              break;
          }
        } else {
          switch (perms.getFeature().getFeature().getValueType()) {
            case BOOLEAN:
              // a null boolean is not valid
              statRecorder.recordHit(key, EdgeHitResultType.UPDATE_NONSENSE, EdgeHitSourceType.TESTSDK);
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
        statRecorder.recordHit(key, EdgeHitResultType.UPDATE_NO_CHANGE, EdgeHitSourceType.TESTSDK);
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

      statRecorder.recordHit(key, EdgeHitResultType.SUCCESS, EdgeHitSourceType.TESTSDK);
      return Response.ok().build();
    } catch (Exception e) {
      log.error("Failed to process request: {}/{}/{}/{} : {}", namedCache, envId, apiKey, featureKey,
        featureStateUpdate, e);

      statRecorder.recordHit(key, EdgeHitResultType.FAILED_TO_PROCESS_REQUEST, EdgeHitSourceType.TESTSDK);
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
    }
  }
}
