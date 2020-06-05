package io.featurehub.client.jersey;

import io.featurehub.client.ClientFeatureRepository;
import io.featurehub.sse.model.SSEResultState;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.media.sse.EventInput;
import org.glassfish.jersey.media.sse.InboundEvent;
import org.glassfish.jersey.media.sse.SseFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.inject.Singleton;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@Singleton
public class JerseyClient {
  private static final Logger log = LoggerFactory.getLogger(JerseyClient.class);
  private final String sdkUrl;
  private final WebTarget target;
  private boolean initialized;
  private final Executor executor;
  private final ClientFeatureRepository clientFeatureRepository;

  public JerseyClient(String sdkUrl, boolean initializeOnConstruction, ClientFeatureRepository clientFeatureRepository) {
    this.sdkUrl = sdkUrl;
    this.clientFeatureRepository = clientFeatureRepository;

    Client client = ClientBuilder.newBuilder()
      .register(JacksonFeature.class)
      .register(SseFeature.class).build();

    target = client.target(sdkUrl);
    executor = Executors.newSingleThreadExecutor();

    if (initializeOnConstruction) {
      init();
    }
  }

  // backoff algorithm should be configurable
  private void avoidServerDdos() {
    try {
      Thread.sleep(10000); // wait 10 seconds
    } catch (InterruptedException e) {
    }
    executor.execute(this::listenUntilDead);
  }

  private void listenUntilDead() {
    long start = System.currentTimeMillis();
    try {
      EventInput eventInput = target.request().get(EventInput.class);

      while (!eventInput.isClosed()) {
        final InboundEvent inboundEvent = eventInput.read();
        initialized = true;
        if (inboundEvent == null) { // connection has been closed
          break;
        }

        clientFeatureRepository.notify(SSEResultState.fromValue(inboundEvent.getName()), inboundEvent.readData());
      }
    } catch (Exception e) {
      log.error("Failed to connect to {}", sdkUrl, e);
      clientFeatureRepository.notify(SSEResultState.ERROR, "unable to connect");
    }

    log.warn("connection failed, reconnecting");
    initialized = false;

    // timeout should be configurable
    if (System.currentTimeMillis() - start < 2000) {
      executor.execute(this::avoidServerDdos);
    } else {
      // if we have fallen out, try again
      executor.execute(this::listenUntilDead);
    }

  }

  public boolean isInitialized() {
    return initialized;
  }

  @PostConstruct
  void init() {
    if (!initialized) {
      executor.execute(this::listenUntilDead);
    }
  }
}
