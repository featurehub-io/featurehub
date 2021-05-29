package io.featurehub.client.edge;

public interface EdgeRetryService {
  void edgeResult(EdgeConnectionState state, EdgeReconnector reconnector);

  void close();
}
