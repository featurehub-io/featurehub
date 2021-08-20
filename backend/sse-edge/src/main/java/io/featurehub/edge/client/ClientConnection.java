package io.featurehub.edge.client;

import io.featurehub.edge.KeyParts;
import io.featurehub.edge.features.FeatureRequestResponse;
import io.featurehub.edge.strategies.ClientContext;
import io.featurehub.mr.model.FeatureValueCacheItem;
import io.featurehub.sse.model.SSEResultState;

import java.io.IOException;
import java.util.UUID;

public interface ClientConnection {
  boolean discovery();

  UUID getEnvironmentId();

  String getApiKey();

  KeyParts getKey();

  ClientContext getClientContext();

  void writeMessage(SSEResultState name, String data) throws IOException;

  void registerEjection(EjectHandler handler);

  void close(boolean sayBye);

  void close();

  String getNamedCache();

  void failed(String reason);

  void initResponse(FeatureRequestResponse edgeResponse);

  // notify the client of a new feature (if they have received their features)
  void notifyFeature(FeatureValueCacheItem rf);
}
