package io.featurehub.edge;

import io.featurehub.edge.client.ClientConnection;

public interface ServerController {
  void listenForFeatureUpdates(String namedCache);

  void unlistenForFeatureUpdates(String namedCache);

  void clientRemoved(ClientConnection client);

  void listenExecutor(Runnable runnable);

  void removeInflightSSEListenerRequest(KeyParts key);
}
