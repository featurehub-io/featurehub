package io.featurehub.edge;

import io.featurehub.edge.client.ClientConnection;
import io.featurehub.mr.messaging.StreamedFeatureUpdate;
import io.featurehub.mr.model.DachaPermissionResponse;

public interface StreamingFeatureController {
  void clientRemoved(ClientConnection client);

  void listenExecutor(Runnable runnable);

  void requestFeatures(ClientConnection b);
}
