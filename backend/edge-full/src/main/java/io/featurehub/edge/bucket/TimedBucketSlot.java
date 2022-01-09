package io.featurehub.edge.bucket;

import io.featurehub.edge.client.ClientConnection;

public interface TimedBucketSlot {
  void addConnection(ClientConnection conn);

  void swapConnection(ClientConnection conn, TimedBucket newBucket);
}
