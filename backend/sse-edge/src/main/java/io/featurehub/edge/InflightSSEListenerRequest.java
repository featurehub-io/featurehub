package io.featurehub.edge;

import io.featurehub.edge.client.ClientConnection;
import io.featurehub.mr.model.EdgeInitResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.concurrent.ConcurrentLinkedQueue;

class InflightSSEListenerRequest {
  private static final Logger log = LoggerFactory.getLogger(InflightSSEListenerRequest.class);
  private final Collection<ClientConnection> sameRequestClients = new ConcurrentLinkedQueue<>();
  private final String key;
  private final ServerController controller;

  InflightSSEListenerRequest(String key, ServerController controller) {
    this.key = key;
    this.controller = controller;
  }

  int add(ClientConnection clientConnection) {
    synchronized (sameRequestClients) {
      int count = sameRequestClients.size();
      sameRequestClients.add(clientConnection);

      if (count == 0) {
        controller.listenForFeatureUpdates(clientConnection.getNamedCache());
      }

      return count;
    }
  }

  void reject() {
    synchronized (sameRequestClients) {
      if (!sameRequestClients.isEmpty()) {
        controller.unlistenForFeatureUpdates(sameRequestClients.stream().findFirst().get().getNamedCache());
        sameRequestClients.forEach(client -> controller.listenExecutor(() -> {
          client.failed("unable to communicate with named cache.");
          controller.clientRemoved(client);
        }));
        sameRequestClients.clear();
      }
    }
  }

  void success(EdgeInitResponse response) {
    synchronized (sameRequestClients) {
      sameRequestClients.forEach(client -> controller.listenExecutor(() -> {
        client.initResponse(response);
        client.registerEjection(controller::clientRemoved);
      }));
      sameRequestClients.clear();
      log.debug("successful response {}", key);
    }
  }

  public void removeCheck() {
    synchronized (sameRequestClients) {
      if (sameRequestClients.isEmpty()) {
        log.debug("no-one asked for this key {}, ejecting", key);
        controller.removeInflightSSEListenerRequest(key);
      }
    }
  }
}
