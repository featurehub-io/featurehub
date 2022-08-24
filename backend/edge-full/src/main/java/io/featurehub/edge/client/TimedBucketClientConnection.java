package io.featurehub.edge.client;

import io.featurehub.dacha.model.PublishAction;
import io.featurehub.dacha.model.PublishFeatureValue;
import io.featurehub.edge.FeatureTransformer;
import io.featurehub.edge.KeyParts;
import io.featurehub.edge.bucket.BucketService;
import io.featurehub.edge.bucket.TimedBucket;
import io.featurehub.edge.bucket.TimedBucketSlot;
import io.featurehub.edge.features.ETagSplitter;
import io.featurehub.edge.features.EtagStructureHolder;
import io.featurehub.edge.features.FeatureRequestResponse;
import io.featurehub.edge.stats.StatRecorder;
import io.featurehub.edge.strategies.ClientContext;
import io.featurehub.jersey.config.CacheJsonMapper;
import io.featurehub.sse.model.SSEResultState;
import io.featurehub.sse.stats.model.EdgeHitResultType;
import io.featurehub.sse.stats.model.EdgeHitSourceType;
import io.prometheus.client.Histogram;
import jakarta.ws.rs.core.Feature;
import jakarta.ws.rs.core.MediaType;
import org.glassfish.jersey.media.sse.EventOutput;
import org.glassfish.jersey.media.sse.OutboundEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class TimedBucketClientConnection implements ClientConnection {
  private static final Logger log = LoggerFactory.getLogger(TimedBucketClientConnection.class);
  @NotNull protected final EventOutput output;
  @NotNull protected final KeyParts apiKey;
  private final Map<UUID, EjectHandler> handlers = new HashMap<>();
  protected final String extraContext;
  @NotNull protected final BucketService bucketService;
  private List<PublishFeatureValue> heldFeatureUpdates = new ArrayList<>();
  @NotNull protected final FeatureTransformer featureTransformer;
  @NotNull protected final ClientContext attributesForStrategy;
  @NotNull protected final StatRecorder statRecorder;
  @NotNull protected final EtagStructureHolder etags;
  @NotNull protected final UUID id;

  private static final Histogram connectionLengthHistogram =
      Histogram.build(
              "edge_conn_length_sse",
              "The length of time that the connection is open for SSE clients")
          .register();

  private final Histogram.Timer timer;
  private TimedBucket timedBucketSlot;

  public TimedBucketClientConnection(
      @NotNull EventOutput output,
      @NotNull KeyParts apiKey,
      @NotNull FeatureTransformer featureTransformer,
      @NotNull StatRecorder statRecorder,
      @Nullable List<String> featureHubAttributes,
      @Nullable String etag,
      @Nullable String extraContext,
      @NotNull BucketService bucketService) {
    this.extraContext = extraContext;
    this.bucketService = bucketService;
    id = UUID.randomUUID();

    this.output = output;
    this.apiKey = apiKey;
    this.featureTransformer = featureTransformer;
    this.statRecorder = statRecorder;

    attributesForStrategy =
        ClientContext.decode(featureHubAttributes, Collections.singletonList(apiKey));

    etags =
        ETagSplitter.Companion.splitTag(etag, List.of(apiKey), attributesForStrategy.makeEtag());

    timer = connectionLengthHistogram.startTimer();
  }

  @Override
  public UUID connectionId() {
    return id;
  }

  @Override
  public boolean discovery() {
    if (!etags.getValidEtag()) {
      try {
        writeMessage(SSEResultState.ACK, SSEStatusMessage.status("discover"));
      } catch (IOException e) {
        return false;
      }
    }

    return true;
  }

  @Override
  public UUID getEnvironmentId() {
    return apiKey.getEnvironmentId();
  }

  @Override
  public String getApiKey() {
    return apiKey.getServiceKey();
  }

  @Override
  public KeyParts getKey() {
    return apiKey;
  }

  @Override
  public void heartbeat() {
    try {
      writeMessage(SSEResultState.ACK, "\"❤️\"");
    } catch (IOException ignored) {
      log.trace("connection dropped when attempting heartbeat");
      close(false);
    }
  }

  @Override
  public ClientContext getClientContext() {
    return attributesForStrategy;
  }

  @Override
  public void writeMessage(SSEResultState name, String data) throws IOException {
    writeMessage(name, null, data);
  }

  public void writeMessage(SSEResultState name, String etags, String data) throws IOException {
    if (!output.isClosed()) {
      final OutboundEvent.Builder eventBuilder = new OutboundEvent.Builder();
      log.trace("data is  etag `{}`: name: `{}` data `{}`", etags, name, data);
      eventBuilder.name(name.toString());
      eventBuilder.mediaType(MediaType.TEXT_PLAIN_TYPE);
      if (etags != null) {
        eventBuilder.id(etags);
      }
      eventBuilder.data(data);
      final OutboundEvent event = eventBuilder.build();
      output.write(event);
    } else {
      notifyHandlersThatTheConnectionHasClosed();
    }
  }

  private boolean notifiedClosed = false;

  private void notifyHandlersThatTheConnectionHasClosed() {
    // tell them we are shutting down even if they told us to shut them down
    if (!notifiedClosed) {
      notifiedClosed = true;

      handlers.values().parallelStream().forEach(e -> e.eject(this));

      timer.observeDuration();
    }
  }

  @Override
  public UUID registerEjection(EjectHandler handler) {
    UUID id = UUID.randomUUID();
    this.handlers.put(id, handler);
    return id;
  }

  @Override
  public void deregisterEjection(UUID id) {
    this.handlers.remove(id);
  }

  @Override
  public void close(boolean sayBye) {
    // could have been closed by a failure earlier, it isn't ejected from the list
    if (!output.isClosed()) {
      if (sayBye) {
        try {
          writeMessage(SSEResultState.BYE, SSEStatusMessage.status("closed"));
        } catch (Exception e) {
          log.warn("Failed to close", e);
        }
      }
      try {
        output.close();
      } catch (Exception ignored) {
      }
    }

    notifyHandlersThatTheConnectionHasClosed();
  }

  @Override
  public void close() {
    close(true);
  }

  @Override
  public String getNamedCache() {
    return apiKey.getCacheName();
  }

  @Override
  public void failed(String reason) {
    try {
      writeMessage(SSEResultState.FAILURE, SSEStatusMessage.status(reason));
      statRecorder.recordHit(
          apiKey, EdgeHitResultType.FAILED_TO_PROCESS_REQUEST, EdgeHitSourceType.EVENTSOURCE);
      close(false);
    } catch (IOException e) {
      log.warn("Failed to fail client connection", e);
      close(false);
    }
  }

  @Override
  public void initResponse(FeatureRequestResponse edgeResponse) {
    try {
      try {
        switch (edgeResponse.getSuccess()) {
          case NO_SUCH_KEY_IN_CACHE:
            failed("Unrecognized API key. Please stop requesting.");
            break;
          case SUCCESS:
            writeMessage(
                SSEResultState.FEATURES,
                ETagSplitter.Companion.makeEtags(
                    etags, Collections.singletonList(edgeResponse.getEtag())),
                CacheJsonMapper.mapper.writeValueAsString(
                    edgeResponse.getEnvironment().getFeatures()));

            statRecorder.recordHit(
                apiKey, EdgeHitResultType.SUCCESS, EdgeHitSourceType.EVENTSOURCE);

            writeHeldFeatureChanges();
            break;
          case NO_CHANGE:
            statRecorder.recordHit(
                apiKey, EdgeHitResultType.SUCCESS, EdgeHitSourceType.EVENTSOURCE);

            writeHeldFeatureChanges();
            break;
          case DACHA_NOT_READY:
            statRecorder.recordHit(apiKey, EdgeHitResultType.MISSED, EdgeHitSourceType.EVENTSOURCE);

            // move it to a random number of buckets ahead to get a kick-out
            bucketService.dachaIsUnavailable(this);
            break;
        }

      } catch (IOException iex) {
        statRecorder.recordHit(
            apiKey, EdgeHitResultType.FAILED_TO_WRITE_ON_INIT, EdgeHitSourceType.EVENTSOURCE);
      }
    } catch (Exception e) {
      failed("Could not write initial features");
    }
  }

  private void writeHeldFeatureChanges() {
    List<PublishFeatureValue> heldUpdates = heldFeatureUpdates;

    heldFeatureUpdates = null;

    if (heldUpdates != null) {
      heldUpdates.forEach(this::notifyFeature);
    }
  }

  // notify the client of a new feature (if they have received their features)
  @Override
  public void notifyFeature(PublishFeatureValue rf) {
    if (heldFeatureUpdates != null) {
      log.debug("holding feature update for client");
      heldFeatureUpdates.add(rf);
    } else {
      try {
        String data =
            CacheJsonMapper.mapper.writeValueAsString(
                featureTransformer.transform(rf.getFeature(), attributesForStrategy));
        // if it was a DELETE or it was being triggered as a retired feature
        if (rf.getAction() == PublishAction.DELETE || (rf.getFeature().getValue() != null && rf.getFeature().getValue().getRetired() == Boolean.TRUE)) {
          writeMessage(SSEResultState.DELETE_FEATURE, data);
        } else {
          writeMessage(SSEResultState.FEATURE, data);
        }
      } catch (IOException e) {
        log.error("Failed to write feature", e);
        close(false);
      }
    }
  }

  @Override
  public EtagStructureHolder etags() {
    return etags;
  }

  @Override
  public void setTimedBucketSlot(TimedBucket timedBucket) {
    this.timedBucketSlot = timedBucket;
  }

  @Override
  public TimedBucketSlot getTimedBucketSlot() {
    return this.timedBucketSlot;
  }
}
