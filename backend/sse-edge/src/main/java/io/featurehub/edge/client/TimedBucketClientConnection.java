package io.featurehub.edge.client;

import io.featurehub.edge.FeatureTransformer;
import io.featurehub.edge.KeyParts;
import io.featurehub.edge.features.ETagSplitter;
import io.featurehub.edge.features.EtagStructureHolder;
import io.featurehub.edge.features.FeatureRequestResponse;
import io.featurehub.edge.features.FeatureRequestSuccess;
import io.featurehub.edge.stats.StatRecorder;
import io.featurehub.edge.strategies.ClientContext;
import io.featurehub.jersey.config.CacheJsonMapper;
import io.featurehub.mr.model.FeatureValueCacheItem;
import io.featurehub.mr.model.PublishAction;
import io.featurehub.sse.model.SSEResultState;
import io.featurehub.sse.stats.model.EdgeHitResultType;
import io.featurehub.sse.stats.model.EdgeHitSourceType;
import io.prometheus.client.Histogram;
import jakarta.ws.rs.core.MediaType;
import org.glassfish.jersey.media.sse.EventOutput;
import org.glassfish.jersey.media.sse.OutboundEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class TimedBucketClientConnection implements ClientConnection {
  private static final Logger log = LoggerFactory.getLogger(TimedBucketClientConnection.class);
  private final EventOutput output;
  private final KeyParts apiKey;
  private final List<EjectHandler> handlers = new ArrayList<>();
  private List<FeatureValueCacheItem> heldFeatureUpdates = new ArrayList<>();
  private final FeatureTransformer featureTransformer;
  private final ClientContext attributesForStrategy;
  private final StatRecorder statRecorder;
  private final EtagStructureHolder etags;

  private static final Histogram connectionLengthHistogram = Histogram.build("edge_conn_length_sse", "The length of " +
    "time that the connection is open for SSE clients").register();

  private final Histogram.Timer timer;

  private TimedBucketClientConnection(Builder builder) {
    output = builder.output;
    apiKey = builder.apiKey;
    statRecorder = builder.statRecorder;
    featureTransformer = builder.featureTransformer;

    attributesForStrategy =
        ClientContext.decode(builder.featureHubAttributes, Collections.singletonList(apiKey));

    etags = ETagSplitter.Companion.splitTag(builder.etag, List.of(apiKey), attributesForStrategy.makeEtag());

    timer = connectionLengthHistogram.startTimer();
  }

  @Override
  public boolean discovery() {
    try {
      writeMessage(SSEResultState.ACK, SSEStatusMessage.status("discover"));
    } catch (IOException e) {
      return false;
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
      log.trace("data is : {}", data);
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

      handlers.parallelStream().forEach(e -> e.eject(this));

      timer.observeDuration();
    }
  }

  @Override
  public void registerEjection(EjectHandler handler) {
    this.handlers.add(handler);
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
      statRecorder.recordHit(apiKey, EdgeHitResultType.FAILED_TO_PROCESS_REQUEST, EdgeHitSourceType.EVENTSOURCE);
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
        if (edgeResponse.getSuccess() != FeatureRequestSuccess.FAILED) {
          if (edgeResponse.getSuccess() == FeatureRequestSuccess.SUCCESS) {
            writeMessage(
              SSEResultState.FEATURES,
              ETagSplitter.Companion.makeEtags(etags, List.of(edgeResponse)),
              CacheJsonMapper.mapper.writeValueAsString(edgeResponse.getEnvironment().getFeatures()));
          }

          statRecorder.recordHit(apiKey, EdgeHitResultType.SUCCESS, EdgeHitSourceType.EVENTSOURCE);

          List<FeatureValueCacheItem> heldUpdates = heldFeatureUpdates;

          heldFeatureUpdates = null;

          if (heldUpdates != null) {
            heldUpdates.forEach(this::notifyFeature);
          }
        } else {
          failed("Unrecognized API Key");
        }
      } catch (IOException iex) {
        statRecorder.recordHit(
          apiKey, EdgeHitResultType.FAILED_TO_WRITE_ON_INIT, EdgeHitSourceType.EVENTSOURCE);
      }
    } catch (Exception e) {
      failed("Could not write initial features");
    }
  }

  // notify the client of a new feature (if they have received their features)
  @Override
  public void notifyFeature(FeatureValueCacheItem rf) {
    if (heldFeatureUpdates != null) {
      heldFeatureUpdates.add(rf);
    } else {
      try {
        String data =
            CacheJsonMapper.mapper.writeValueAsString(
                featureTransformer.transform(rf, attributesForStrategy));
        if (rf.getAction() == PublishAction.DELETE) {
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

  public static final class Builder {
    private EventOutput output;
    private KeyParts apiKey;
    private List<String> featureHubAttributes;
    private FeatureTransformer featureTransformer;
    private StatRecorder statRecorder;
    private String etag;

    public Builder() {}

    public Builder statRecorder(StatRecorder val) {
      statRecorder = val;
      return this;
    }

    public Builder output(EventOutput val) {
      output = val;
      return this;
    }

    public Builder apiKey(KeyParts val) {
      apiKey = val;
      return this;
    }

    public Builder featureTransformer(FeatureTransformer val) {
      featureTransformer = val;
      return this;
    }

    public Builder featureHubAttributes(List<String> val) {
      featureHubAttributes = val;
      return this;
    }

    public ClientConnection build() {
      return new TimedBucketClientConnection(this);
    }

    public Builder etag(String etag) {
      this.etag = etag;
      return this;
    }
  }
}
