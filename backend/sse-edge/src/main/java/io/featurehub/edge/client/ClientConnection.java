package io.featurehub.edge.client;

import io.featurehub.mr.model.EdgeInitResponse;
import io.featurehub.mr.model.FeatureValueCacheItem;
import io.featurehub.sse.model.SSEResultState;

import java.io.IOException;

public interface ClientConnection {
  boolean discovery();

  String getEnvironmentId();

  String getApiKey();

  void writeMessage(SSEResultState name, String data) throws IOException;

  void registerEjection(EjectHandler handler);

  void close(boolean sayBye);

  void close();

  String getNamedCache();

  void failed(String reason);

  void initResponse(EdgeInitResponse edgeResponse);

  // notify the client of a new feature (if they have received their features)
  void notifyFeature(FeatureValueCacheItem rf);
}
