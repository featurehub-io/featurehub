package io.featurehub.edge.client;

import io.featurehub.dacha.api.CacheJsonMapper;
import io.featurehub.edge.FeatureTransformer;
import io.featurehub.mr.model.EdgeInitResponse;
import io.featurehub.mr.model.FeatureValueCacheItem;
import io.featurehub.mr.model.PublishAction;
import io.featurehub.sse.model.SSEResultState;
import org.glassfish.jersey.media.sse.EventOutput;
import org.glassfish.jersey.media.sse.OutboundEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class TimedBucketClientConnection implements ClientConnection {
  private static final Logger log = LoggerFactory.getLogger(TimedBucketClientConnection.class);
  private EventOutput output;
  private String environmentId;
  private String apiKey;
  private String namedCache;
  private List<EjectHandler> handlers = new ArrayList<>();
  private List<FeatureValueCacheItem> heldFeatureUpdates = new ArrayList<>();
  private FeatureTransformer featureTransformer;

  private TimedBucketClientConnection(Builder builder) {
    output = builder.output;
    environmentId = builder.environmentId;
    apiKey = builder.apiKey;
    namedCache = builder.namedCache;
    featureTransformer = builder.featureTransformer;
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
  public String getEnvironmentId() {
    return environmentId;
  }

  @Override
  public String getApiKey() {
    return apiKey;
  }

  @Override
  public void writeMessage(SSEResultState name, String data) throws IOException {
    final OutboundEvent.Builder eventBuilder = new OutboundEvent.Builder();
    log.trace("data is : {}", data);
    eventBuilder.name(name.toString());
    eventBuilder.mediaType(MediaType.TEXT_PLAIN_TYPE);
    eventBuilder.data(data);
    final OutboundEvent event = eventBuilder.build();
    output.write(event);
  }

  @Override
  public void registerEjection(EjectHandler handler) {
    this.handlers.add(handler);
  }

  @Override
  public void close(boolean sayBye) {
    // could have been closed by a failure earlier, it isn't ejected from the list
    if (!output.isClosed()) {
      // tell them we are shutting down
      handlers.parallelStream().forEach(e -> e.eject(this));
      if (sayBye) {
        try {
          writeMessage(SSEResultState.BYE, SSEStatusMessage.status("closed"));
        } catch (Exception e) {
          log.warn("Failed to close", e);
        }
      }
      try {
        output.close();
      } catch (Exception e) {
      }
    }
  }

  @Override
  public void close() {
    close(true);
  }

  @Override
  public String getNamedCache() {
    return namedCache;
  }

  @Override
  public void failed(String reason) {
    try {
      writeMessage(SSEResultState.FAILURE, SSEStatusMessage.status(reason));
      close(false);
    } catch (IOException e) {
      log.warn("Failed to fail client connection", e);
      close(false);
    }
  }

  @Override
  public void initResponse(EdgeInitResponse edgeResponse) {
    if (Boolean.TRUE.equals(edgeResponse.getSuccess())) {
      try {
        writeMessage(SSEResultState.FEATURES,
          CacheJsonMapper.mapper.writeValueAsString(featureTransformer.transform(edgeResponse.getFeatures())));
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

  // notify the client of a new feature (if they have received their features)
  @Override
  public void notifyFeature(FeatureValueCacheItem rf) {
    if (heldFeatureUpdates != null) {
      heldFeatureUpdates.add(rf);
    } else {
      try {
        String data = CacheJsonMapper.mapper.writeValueAsString(featureTransformer.transform(rf));
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


  public static final class Builder {
    private EventOutput output;
    private String environmentId;
    private String apiKey;
    private String namedCache;
    private FeatureTransformer featureTransformer;

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

    public Builder featureTransformer(FeatureTransformer val) {
      featureTransformer = val;
      return this;
    }

    public ClientConnection build() {
      return new TimedBucketClientConnection(this);
    }
  }
}
