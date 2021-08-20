package io.featurehub.edge;

import io.featurehub.edge.client.ClientConnection;
import io.featurehub.edge.features.FeatureRequestResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.concurrent.ConcurrentLinkedQueue;

class InflightSSEListenerRequest {
//  private static final Logger log = LoggerFactory.getLogger(InflightSSEListenerRequest.class);
//  private final Collection<ClientConnection> sameRequestClients = new ConcurrentLinkedQueue<>();
//  private final KeyParts key;
//  private final StreamingFeatureController controller;
//
//  InflightSSEListenerRequest(KeyParts key, StreamingFeatureController controller) {
//    this.key = key;
//    this.controller = controller;
//  }
//
//  int add(ClientConnection clientConnection) {
//    synchronized (sameRequestClients) {
//      int count = sameRequestClients.size();
//      sameRequestClients.add(clientConnection);
//
//      if (count == 0) {
//        controller.listenForFeatureUpdates(clientConnection.getNamedCache());
//      }
//
//      return count;
//    }
//  }
//
//  void reject() {
//    log.debug("key {} was invalid, rejecting clients", key);
//    synchronized (sameRequestClients) {
//      if (!sameRequestClients.isEmpty()) {
//        controller.unlistenForFeatureUpdates(sameRequestClients.stream().findFirst().get().getNamedCache());
//
//        sameRequestClients.forEach(client -> controller.listenExecutor(() -> {
//
//          controller.clientRemoved(client);
//        }));
//
//        sameRequestClients.clear();
//      }
//    }
//  }
//
//  void success(final FeatureRequestResponse environment) {
//    synchronized (sameRequestClients) {
//      sameRequestClients.forEach(client -> controller.listenExecutor(() -> {
//        client.initResponse(environment);
//        client.registerEjection(controller::clientRemoved);
//      }));
//      sameRequestClients.clear();
//      log.debug("successful response {}", key);
//    }
//  }
//
//  public void removeCheck() {
//    synchronized (sameRequestClients) {
//      if (sameRequestClients.isEmpty()) {
//        log.debug("All clients in key bucket {} have gone away, ejecting", key);
//        controller.removeInflightSSEListenerRequest(key);
//      }
//    }
//  }
}
