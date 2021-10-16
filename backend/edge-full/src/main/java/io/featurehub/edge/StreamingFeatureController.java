package io.featurehub.edge;

import io.featurehub.edge.client.ClientConnection;

public interface StreamingFeatureController {
  void clientRemoved(ClientConnection client);

  void listenExecutor(Runnable runnable);

  void requestFeatures(ClientConnection b);
}
