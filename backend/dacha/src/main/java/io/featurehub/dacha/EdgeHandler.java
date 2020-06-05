package io.featurehub.dacha;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.featurehub.dacha.api.CacheJsonMapper;
import io.featurehub.mr.model.EdgeInitRequest;
import io.featurehub.mr.model.EdgeInitResponse;
import io.featurehub.mr.model.FeatureValueCacheItem;
import io.nats.client.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

public class EdgeHandler implements IncomingEdgeRequest {
  private static final Logger log = LoggerFactory.getLogger(EdgeHandler.class);
  private final InternalCache cache;

  public EdgeHandler(InternalCache cache) {
    this.cache = cache;
  }

  @Override
  public byte[] request(Message message) throws InterruptedException {
    final EdgeInitResponse response = new EdgeInitResponse().success(false);

    try {
      edgeInitRequest(CacheJsonMapper.mapper.readValue(message.getData(), EdgeInitRequest.class), response);
    } catch (IOException e) {
      log.warn("Unrecognized request from Edge", e);
    }

    try {
      return CacheJsonMapper.mapper.writeValueAsBytes(response);
    } catch (JsonProcessingException e) {
      throw new InterruptedException();
    }
  }

  private void edgeInitRequest(EdgeInitRequest request, EdgeInitResponse response) {
    Collection<FeatureValueCacheItem> features = cache.getFeaturesByEnvironmentAndServiceAccount(request.getEnvironmentId(), request.getApiKey());

    response.success(features != null)
      .features(features == null ? null : new ArrayList<>(features))
      .environmentId(request.getEnvironmentId())
      .apiKey(request.getApiKey());
  }


}
