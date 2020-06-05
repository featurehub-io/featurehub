package io.featurehub.edge.client;

// used by a timed bucket to eject itself from the server config that is listening
public interface EjectHandler {
  void eject(TimedBucketClientConnection me);
}
