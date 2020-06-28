package io.featurehub.dacha;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.featurehub.dacha.api.CacheJsonMapper;
import io.featurehub.mr.model.EdgeInitPermissionResponse;
import io.featurehub.mr.model.EdgeInitRequest;
import io.featurehub.mr.model.EdgeInitRequestCommand;
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
    Object response;

    try {
      final EdgeInitRequest request = CacheJsonMapper.mapper.readValue(message.getData(), EdgeInitRequest.class);
      if (request.getCommand() == EdgeInitRequestCommand.LISTEN) {
        final EdgeInitResponse success = new EdgeInitResponse().success(false);
        edgeInitRequest(request, success);
        response = success;
      } else {
        final EdgeInitPermissionResponse perms = new EdgeInitPermissionResponse().success(false);
        editPermissionRequest(request, perms);
        response = perms;
      }
    } catch (IOException e) {
      log.warn("Unrecognized request from Edge", e);
      response = new EdgeInitResponse().success(false);
    }

    try {
      return CacheJsonMapper.mapper.writeValueAsBytes(response);
    } catch (JsonProcessingException e) {
      throw new InterruptedException();
    }
  }

  private void editPermissionRequest(EdgeInitRequest request, EdgeInitPermissionResponse response) {
    InternalCache.FeatureCollection features = cache.getFeaturesByEnvironmentAndServiceAccount(request.getEnvironmentId(), request.getApiKey());

    if (features != null) {
      response
        .feature(features.features.stream().filter(f -> f.getFeature().getKey().equals(request.getFeatureKey())).findFirst().orElse(null))
        .roles(features.perms.getPermissions());
      response.success(response.getFeature() != null);
    }

  }

  private void edgeInitRequest(EdgeInitRequest request, EdgeInitResponse response) {
    InternalCache.FeatureCollection features = cache.getFeaturesByEnvironmentAndServiceAccount(request.getEnvironmentId(), request.getApiKey());

    response.success(features != null)
      .features(features == null ? null : new ArrayList<>(features.features))
      .environmentId(request.getEnvironmentId())
      .apiKey(request.getApiKey());
  }


}
