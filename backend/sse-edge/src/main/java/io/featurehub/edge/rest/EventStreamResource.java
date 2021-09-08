package io.featurehub.edge.rest;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.featurehub.edge.FeatureTransformer;
import io.featurehub.edge.KeyParts;
import io.featurehub.edge.StreamingFeatureController;
import io.featurehub.edge.bucket.EventOutputBucketService;
import io.featurehub.edge.client.ClientConnection;
import io.featurehub.edge.client.TimedBucketClientConnection;
import io.featurehub.edge.features.DachaFeatureRequestSubmitter;
import io.featurehub.edge.features.ETagSplitter;
import io.featurehub.edge.features.EtagStructureHolder;
import io.featurehub.edge.features.FeatureRequestResponse;
import io.featurehub.edge.features.FeatureRequestSuccess;
import io.featurehub.edge.permission.PermissionPublisher;
import io.featurehub.edge.stats.StatRecorder;
import io.featurehub.edge.strategies.ClientContext;
import io.featurehub.edge.utils.UpdateMapper;
import io.featurehub.mr.messaging.StreamedFeatureUpdate;
import io.featurehub.mr.model.DachaPermissionResponse;
import io.featurehub.mr.model.FeatureValue;
import io.featurehub.mr.model.RoleType;
import io.featurehub.sse.model.FeatureStateUpdate;
import io.featurehub.sse.stats.model.EdgeHitResultType;
import io.featurehub.sse.stats.model.EdgeHitSourceType;
import io.prometheus.client.Gauge;
import io.prometheus.client.Histogram;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.container.AsyncResponse;
import jakarta.ws.rs.container.Suspended;
import jakarta.ws.rs.core.Response;
import org.glassfish.hk2.api.Immediate;
import org.glassfish.jersey.media.sse.EventOutput;
import org.glassfish.jersey.media.sse.SseFeature;
import org.glassfish.jersey.server.ManagedAsync;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Path("/features")
@Immediate
public class EventStreamResource {
  private static final Logger log = LoggerFactory.getLogger(EventStreamResource.class);

  private final EventOutputBucketService bucketService;
  private final StreamingFeatureController serverConfig;
  private final StatRecorder statRecorder;
  private final DachaFeatureRequestSubmitter getOrchestrator;
  private final FeatureTransformer featureTransformer;
  private final UpdateMapper updateMapper;
  private final PermissionPublisher permissionPublisher;

  // we are doing timers here rather than instrumenting Jersey because in this case the names are more interesting and
  // useful in the sea of metrics
  private static final Histogram pollSpeedHistogram = Histogram.build("edge_conn_length_poll", "The length of " +
    "time that the connection is open for Polling clients").register();
  private static final Histogram testSpeedHistogram = Histogram.build("edge_conn_length_test", "The length of " +
    "time that the connection is open for Testing clients").register();

  @Inject
  public EventStreamResource(EventOutputBucketService bucketService, StreamingFeatureController serverConfig,
                             StatRecorder statRecorder, DachaFeatureRequestSubmitter getOrchestrator,
                             FeatureTransformer featureTransformer, UpdateMapper updateMapper, PermissionPublisher permissionPublisher) {
    this.bucketService = bucketService;
    this.serverConfig = serverConfig;
    this.statRecorder = statRecorder;
    this.getOrchestrator = getOrchestrator;
    this.featureTransformer = featureTransformer;
    this.updateMapper = updateMapper;
    this.permissionPublisher = permissionPublisher;
  }

  static Gauge inout = Gauge.build("edge_get_req", "how many GET requests").register();

  // support new and old style for GET - apiKeys and sdkUrl, they are the same we just want to transition the
  // naming at some point
  @GET
  @Path("/")
  @Produces({ "application/json" })
  @ManagedAsync
  public void getFeatureStates(@Suspended AsyncResponse response, @QueryParam("sdkUrl") List<String> sdkUrls,
                               @QueryParam("apiKey") List<String> apiKeys,
                               @HeaderParam("x-featurehub") List<String> featureHubAttrs,
                               @HeaderParam("etag") String etagHeader) {
    if ((sdkUrls == null || sdkUrls.isEmpty()) && (apiKeys == null || apiKeys.isEmpty()) ) {
      response.resume(new BadRequestException());
      return;
    }

    inout.inc();

    final Histogram.Timer timer = pollSpeedHistogram.startTimer();

    final List<KeyParts> realApiKeys =
      (sdkUrls == null || sdkUrls.isEmpty() ? apiKeys : sdkUrls)
        .stream()
        .distinct() // we want unique ones
        .map(KeyParts.Companion::fromString)
        .filter(Objects::nonNull).collect(Collectors.toList());

    if (realApiKeys.isEmpty()) {
      response.resume(new NotFoundException());
    }

    final ClientContext clientContext = ClientContext.decode(featureHubAttrs, realApiKeys);
    final EtagStructureHolder etags = ETagSplitter.Companion.splitTag(etagHeader, realApiKeys, clientContext.makeEtag());

    final List<FeatureRequestResponse> environments = getOrchestrator.request(realApiKeys, clientContext, etags);

    // record the result
    environments.forEach(resp -> statRecorder.recordHit(resp.getKey(), mapSuccess(resp.getSuccess()),
      EdgeHitSourceType.POLL));

    timer.observeDuration();

    inout.dec();

    if (environments.get(0).getSuccess() == FeatureRequestSuccess.NO_CHANGE) {
      response.resume(Response.status(304).header("etag", etagHeader).build());
    } else {
      response.resume(Response.status(200)
          .header("etag", ETagSplitter.Companion.makeEtags(etags, environments))
        .entity(environments.stream().map(FeatureRequestResponse::getEnvironment).collect(Collectors.toList()))
        .build());
    }


  }

  private EdgeHitResultType mapSuccess(FeatureRequestSuccess success) {
    switch (success) {
      case FAILED:
        return EdgeHitResultType.MISSED;
      case SUCCESS:
        return EdgeHitResultType.SUCCESS;
      case NO_CHANGE:
        return EdgeHitResultType.NO_CHANGE;
    }

    return EdgeHitResultType.SUCCESS;
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
  @ManagedAsync
  public void update(@Suspended AsyncResponse response, @PathParam("namedCache") String namedCache,
                         @PathParam("environmentId") UUID envId,
                         @PathParam("apiKey") String apiKey,
                         @PathParam("featureKey") String featureKey,
                         FeatureStateUpdate featureStateUpdate) {

    Histogram.Timer timer = testSpeedHistogram.startTimer();

    try {
      testAPi(response, namedCache, envId, apiKey, featureKey, featureStateUpdate);
    } finally{
      timer.observeDuration();
    }
  }

  private void testAPi(AsyncResponse response, String namedCache, UUID envId, String apiKey, String featureKey,
                           FeatureStateUpdate featureStateUpdate) {
    final KeyParts key = new KeyParts(namedCache, envId, apiKey);

    try {
      final DachaPermissionResponse perms = permissionPublisher.requestPermission(key, featureKey);

      if (perms == null) {
        statRecorder.recordHit(key, EdgeHitResultType.MISSED, EdgeHitSourceType.TESTSDK);
        response.resume(Response.status(Response.Status.NOT_FOUND).build());
        return;
      }

      key.setApplicationId(perms.getApplicationId());
      key.setOrganisationId(perms.getOrganizationId());
      key.setPortfolioId(perms.getPortfolioId());
      key.setServiceKeyId(perms.getServiceKeyId());

      if (perms.getRoles().isEmpty() || (perms.getRoles().size() == 1 && perms.getRoles().get(0) == RoleType.READ)) {
        statRecorder.recordHit(key, EdgeHitResultType.FORBIDDEN, EdgeHitSourceType.TESTSDK);
        response.resume(Response.status(Response.Status.FORBIDDEN).build());
        return;
      }

      if (Boolean.TRUE.equals(featureStateUpdate.getLock())) {
        if (!perms.getRoles().contains(RoleType.LOCK)) {
          statRecorder.recordHit(key, EdgeHitResultType.FORBIDDEN, EdgeHitSourceType.TESTSDK);
          response.resume(Response.status(Response.Status.FORBIDDEN).build());
          return;
        }
      } else if (Boolean.FALSE.equals(featureStateUpdate.getLock())) {
        if (!perms.getRoles().contains(RoleType.UNLOCK)) {
          statRecorder.recordHit(key, EdgeHitResultType.FORBIDDEN, EdgeHitSourceType.TESTSDK);
          response.resume(Response.status(Response.Status.FORBIDDEN).build());
          return;
        }
      }

      if (featureStateUpdate.getValue() != null) {
        featureStateUpdate.setUpdateValue(Boolean.TRUE);
      }

      // nothing to do?
      if (featureStateUpdate.getLock() == null && (featureStateUpdate.getUpdateValue() == null || Boolean.FALSE.equals(featureStateUpdate.getUpdateValue()) )) {
        statRecorder.recordHit(key, EdgeHitResultType.FAILED_TO_PROCESS_REQUEST, EdgeHitSourceType.TESTSDK);
        response.resume(Response.status(Response.Status.BAD_REQUEST).build());
        return;
      }

      if (Boolean.TRUE.equals(featureStateUpdate.getUpdateValue())) {
        if (!perms.getRoles().contains(RoleType.CHANGE_VALUE)) {
          statRecorder.recordHit(key, EdgeHitResultType.FORBIDDEN, EdgeHitSourceType.TESTSDK);
          response.resume(Response.status(Response.Status.FORBIDDEN).build());
          return;
        } else if (Boolean.TRUE.equals(perms.getFeature().getValue().getLocked()) && !Boolean.FALSE.equals(featureStateUpdate.getLock())) {
          // its locked, and you are trying to change its value and not unlocking it at the same time, that makes no
          // sense
          statRecorder.recordHit(key, EdgeHitResultType.UPDATE_NONSENSE, EdgeHitSourceType.TESTSDK);
          response.resume(Response.status(Response.Status.PRECONDITION_FAILED).build());
          return;
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
                response.resume(Response.status(Response.Status.BAD_REQUEST).build());
                return;
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
                updateMapper.getMapper().readTree(val);
              } catch (JsonProcessingException jpe) {
                statRecorder.recordHit(key, EdgeHitResultType.FAILED_TO_PROCESS_REQUEST, EdgeHitSourceType.TESTSDK);
                response.resume(Response.status(Response.Status.BAD_REQUEST).build());
                return;
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
                response.resume(Response.status(Response.Status.BAD_REQUEST).build());
                return;
              }
              break;
          }
        } else {
          switch (perms.getFeature().getFeature().getValueType()) {
            case BOOLEAN:
              // a null boolean is not valid
              statRecorder.recordHit(key, EdgeHitResultType.UPDATE_NONSENSE, EdgeHitSourceType.TESTSDK);
              response.resume(Response.status(Response.Status.PRECONDITION_FAILED).build());
              return;
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
        response.resume(Response.status(Response.Status.ACCEPTED).build());
        return;
      }

      if (valueNotActuallyChanging) {
        upd.updatingValue(null);
        upd.valueBoolean(null);
        upd.valueNumber(null);
        upd.valueString(null);
      }

      log.debug("publishing update on {} for {}", namedCache, upd);
      permissionPublisher.publishFeatureChangeRequest(upd, namedCache);

      statRecorder.recordHit(key, EdgeHitResultType.SUCCESS, EdgeHitSourceType.TESTSDK);
      response.resume(Response.ok().build());
    } catch (Exception e) {
      log.error("Failed to process request: {}/{}/{}/{} : {}", namedCache, envId, apiKey, featureKey,
        featureStateUpdate, e);

      statRecorder.recordHit(key, EdgeHitResultType.FAILED_TO_PROCESS_REQUEST, EdgeHitSourceType.TESTSDK);
      response.resume(Response.status(Response.Status.INTERNAL_SERVER_ERROR).build());
    }
  }
}
