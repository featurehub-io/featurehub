package io.featurehub.client.edge;

public interface EdgeRetryService {
  void edgeResult(EdgeConnectionState state, EdgeReconnector reconnector);

  void close();

  int getServerConnectTimeoutMs();

  int getServerDisconnectRetryMs();

  int getServerByeReconnectMs();

  int getMaximumBackoffTimeMs();

  int getCurrentBackoffMultiplier();

  int getBackoffMultiplier();

  boolean isNotFoundState();
}
