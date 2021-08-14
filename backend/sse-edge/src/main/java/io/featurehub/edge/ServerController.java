package io.featurehub.edge;

import io.featurehub.edge.client.ClientConnection;
import io.featurehub.mr.messaging.StreamedFeatureUpdate;
import io.featurehub.mr.model.DachaPermissionResponse;

public interface ServerController {
  void listenForFeatureUpdates(String namedCache);

  void unlistenForFeatureUpdates(String namedCache);

  void clientRemoved(ClientConnection client);

  void listenExecutor(Runnable runnable);

  void removeInflightSSEListenerRequest(KeyParts key);

  void requestFeatures(ClientConnection b);

  DachaPermissionResponse requestPermission(KeyParts key, String featureKey);

  void publishFeatureChangeRequest(StreamedFeatureUpdate upd, String namedCache);
}
