package io.featurehub.edge.client;

import io.featurehub.dacha.api.CacheJsonMapper;
import io.featurehub.mr.model.EdgeInitResponse;
import io.featurehub.mr.model.FeatureValueCacheItem;
import io.featurehub.mr.model.FeatureValueType;
import io.featurehub.mr.model.PublishAction;
import io.featurehub.sse.model.FeatureState;
import io.featurehub.sse.model.SSEResultState;
import org.glassfish.jersey.media.sse.EventOutput;
import org.glassfish.jersey.media.sse.OutboundEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class TimedBucketClientConnection {
  private static final Logger log = LoggerFactory.getLogger(TimedBucketClientConnection.class);
  private EventOutput output;
  private String environmentId;
  private String apiKey;
  private String namedCache;
  private List<EjectHandler> handlers = new ArrayList<>();
  private List<FeatureValueCacheItem> heldFeatureUpdates = new ArrayList<>();

  private TimedBucketClientConnection(Builder builder) {
    output = builder.output;
    environmentId = builder.environmentId;
    apiKey = builder.apiKey;
    namedCache = builder.namedCache;
  }

  public boolean discovery() {
    try {
      writeMessage(SSEResultState.ACK, SSEStatusMessage.status("discover"));
    } catch (IOException e) {
      return false;
    }

    return true;
  }

  public String getEnvironmentId() {
    return environmentId;
  }

  public String getApiKey() {
    return apiKey;
  }

  public void writeMessage(SSEResultState name, String data) throws IOException {
    final OutboundEvent.Builder eventBuilder = new OutboundEvent.Builder();
    log.info("data is : {}", data);
    eventBuilder.name(name.toString());
    eventBuilder.mediaType(MediaType.TEXT_PLAIN_TYPE);
    eventBuilder.data(data);
    final OutboundEvent event = eventBuilder.build();
    output.write(event);
  }

  public void registerEjection(EjectHandler handler) {
    this.handlers.add(handler);
  }

  public void close() {
    // could have been closed by a failure earlier, it isn't ejected from the list
    if (!output.isClosed()) {
      // tell them we are shutting down
      handlers.parallelStream().forEach(e -> e.eject(this));
      try {
        writeMessage(SSEResultState.BYE, SSEStatusMessage.status("closed"));
        output.close();
      } catch (Exception e) {
        log.warn("Failed to close", e);
      }
    }
  }

  public String getNamedCache() {
    return namedCache;
  }

  public void failed(String reason) {
    try {
      writeMessage(SSEResultState.FAILURE, SSEStatusMessage.status(reason));
      close();
    } catch (IOException e) {
      log.warn("Failed to fail client connection", e);
      close();
    }
  }

  public void initResponse(EdgeInitResponse edgeResponse) {
    if (Boolean.TRUE.equals(edgeResponse.getSuccess())) {
      try {
        writeMessage(SSEResultState.FEATURES, CacheJsonMapper.mapper.writeValueAsString(transform(edgeResponse.getFeatures())));
        List<FeatureValueCacheItem> heldUpdates = heldFeatureUpdates;
        heldFeatureUpdates = null;
        if (heldUpdates != null) {
          heldUpdates.forEach(this::notifyFeature);
        }
      } catch (Exception e) {
        failed("Could not write initial features");
      }
    } else {
      failed("Invalid combination of environment, apiKey, named cache or not yet initialized.");
    }
  }

  private List<FeatureState> transform(List<FeatureValueCacheItem> features) {
    return features.stream().map(this::transform).collect(Collectors.toList());
  }

  // notify the client of a new feature (if they have received their features)
  public void notifyFeature(FeatureValueCacheItem rf) {
    if (heldFeatureUpdates != null) {
      heldFeatureUpdates.add(rf);
    } else {
      try {
        String data = CacheJsonMapper.mapper.writeValueAsString(transform(rf));
        if (rf.getAction() == PublishAction.DELETE) {
          writeMessage(SSEResultState.DELETE_FEATURE, data);
        } else {
          writeMessage(SSEResultState.FEATURE, data);
        }
      } catch (IOException e) {
        log.error("Failed to write feature", e);
        close();
      }
    }
  }

  private FeatureState transform(FeatureValueCacheItem rf) {

    // todo: should also do rollout strategy
    FeatureState fs = new FeatureState()
//      .key(rf.getFeature().getAlias() != null ? rf.getFeature().getAlias() : rf.getFeature().getKey())
      .key(rf.getFeature().getKey())
      .type(io.featurehub.sse.model.FeatureValueType.fromValue(rf.getFeature().getValueType().toString())) // they are the same
      .id(rf.getFeature().getId())
//      .version(rf.getValue() != null ? rf.getValue().getVersion() : null)
      .value(valueAsObject(rf));

    if (rf.getValue() == null || rf.getValue().getVersion() == null) {
      fs.setVersion(0L);
    } else {
      fs.setVersion(rf.getValue().getVersion());
    }

    log.debug("transforming: {} into {}", rf, fs);

    return fs;
  }

  private Object valueAsObject(FeatureValueCacheItem rf) {
    if (rf.getValue() == null)
      return null;

    final FeatureValueType valueType = rf.getFeature().getValueType();
    if (FeatureValueType.BOOLEAN.equals(valueType)) {
      return rf.getValue().getValueBoolean();
    }

    if (FeatureValueType.JSON.equals(valueType)) {
      return rf.getValue().getValueJson();
    }

    if ( FeatureValueType.STRING.equals(valueType)) {
      return rf.getValue().getValueString();
    }

    if (FeatureValueType.NUMBER.equals(valueType)) {
      return rf.getValue().getValueNumber();
    }

    log.error("unknown feature value type, sending null: {}: {}", rf.getFeature().getId(), valueType);

    return null;
  }

  public static final class Builder {
    private EventOutput output;
    private String environmentId;
    private String apiKey;
    private String namedCache;

    public Builder() {
    }

    public Builder output(EventOutput val) {
      output = val;
      return this;
    }

    public Builder environmentId(String val) {
      environmentId = val;
      return this;
    }

    public Builder apiKey(String val) {
      apiKey = val;
      return this;
    }

    public Builder namedCache(String val) {
      namedCache = val;
      return this;
    }

    public TimedBucketClientConnection build() {
      return new TimedBucketClientConnection(this);
    }
  }
}
