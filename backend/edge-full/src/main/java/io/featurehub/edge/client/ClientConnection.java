package io.featurehub.edge.client;

import io.featurehub.dacha.model.PublishFeatureValue;
import io.featurehub.edge.KeyParts;
import io.featurehub.edge.bucket.TimedBucket;
import io.featurehub.edge.bucket.TimedBucketSlot;
import io.featurehub.edge.features.EtagStructureHolder;
import io.featurehub.edge.features.FeatureRequestResponse;
import io.featurehub.edge.strategies.ClientContext;
import io.featurehub.sse.model.SSEResultState;

import java.io.IOException;
import java.util.UUID;

public interface ClientConnection {
  UUID connectionId();

  boolean discovery();

  UUID getEnvironmentId();

  String getApiKey();

  KeyParts getKey();

  void heartbeat();

  ClientContext getClientContext();

  void writeMessage(SSEResultState name, String data) throws IOException;

  UUID registerEjection(EjectHandler handler);
  void deregisterEjection(UUID handle);

  void close(boolean sayBye);

  void close();

  String getNamedCache();

  void failed(String reason);

  void initResponse(FeatureRequestResponse edgeResponse);

  // notify the client of a new feature (if they have received their features)
  void notifyFeature(PublishFeatureValue rf);

  EtagStructureHolder etags();

  void setTimedBucketSlot(TimedBucket timedBucket);

  TimedBucketSlot getTimedBucketSlot();
}
