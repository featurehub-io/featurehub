package io.featurehub.edge.bucket;

import io.featurehub.edge.client.ClientConnection;

public interface BucketService {
  void putInBucket(ClientConnection b);
  void dachaIsUnavailable(ClientConnection b);
}
