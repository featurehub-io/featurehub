package io.featurehub.dacha;

import io.nats.client.Message;

public interface IncomingEdgeRequest {
  byte[] request(Message message) throws InterruptedException;
}
