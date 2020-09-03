package io.featurehub.client.jersey;

import cd.connect.openapi.support.ApiClient;
import io.featurehub.client.ClientContext;
import io.featurehub.client.FeatureRepository;
import io.featurehub.client.Feature;
import io.featurehub.sse.api.FeatureService;
import io.featurehub.sse.model.FeatureStateUpdate;
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
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@Singleton
public class JerseyClient implements ClientContext.ClientContextChanged {
  private static final Logger log = LoggerFactory.getLogger(JerseyClient.class);
  protected final String sdkUrl;
  private final WebTarget target;
  private boolean initialized;
  private final Executor executor;
  private final FeatureRepository featureRepository;
  private final FeatureService featuresService;
  private boolean shutdown = false;
  private boolean shutdownOnServerFailure = true;
  private boolean shutdownOnEdgeFailureConnection = false;
  private EventInput eventInput;
  private String xFeaturehubHeader;

  public JerseyClient(String sdkUrl, boolean initializeOnConstruction, FeatureRepository featureRepository) {
    this(sdkUrl, initializeOnConstruction, featureRepository, null);
  }

  public JerseyClient(String sdkUrl, boolean initializeOnConstruction,
                      FeatureRepository featureRepository, ApiClient apiClient) {
    this.featureRepository = featureRepository;

    Client client = ClientBuilder.newBuilder()
      .register(JacksonFeature.class)
      .register(SseFeature.class).build();

    target = makeEventSourceTarget(client, sdkUrl);
    executor = makeExecutor();

    if (apiClient == null) {
      String basePath = sdkUrl.substring(0, sdkUrl.indexOf("/features"));
      apiClient = new ApiClient(client, basePath);
    }

    this.sdkUrl = sdkUrl.substring(sdkUrl.indexOf("/features/") + 1);

    featuresService = makeFeatureServiceClient(apiClient);

    this.featureRepository.clientContext().registerChangeListener(this);

    if (initializeOnConstruction) {
      init();
    }
  }

  protected Executor makeExecutor() {
    return Executors.newSingleThreadExecutor();
  }

  protected WebTarget makeEventSourceTarget(Client client, String sdkUrl) {
    return client.target(sdkUrl);
  }

  protected FeatureService makeFeatureServiceClient(ApiClient apiClient) {
    return new FeatureServiceImpl(apiClient);
  }

  public void setFeatureState(String key, FeatureStateUpdate update) {
    featuresService.setFeatureState(sdkUrl, key, update);
  }

  public void setFeatureState(Feature feature, FeatureStateUpdate update) {
    setFeatureState(feature.name(), update);
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
      Invocation.Builder request = target.request();

      if (xFeaturehubHeader != null) {
        request = request.header("x-featurehub", xFeaturehubHeader);
      }

      eventInput = request
            .get(EventInput.class);

      while (!eventInput.isClosed()) {
        final InboundEvent inboundEvent = eventInput.read();
        initialized = true;
        if (inboundEvent == null) { // connection has been closed
          break;
        }

        final SSEResultState state = SSEResultState.fromValue(inboundEvent.getName());
        featureRepository.notify(state, inboundEvent.readData());

        if (state == SSEResultState.FAILURE && shutdownOnServerFailure) {
          log.warn("Failed to connect to FeatureHub Edge, shutting down.");
          shutdown();
        }
      }
    } catch (Exception e) {
      log.warn("Failed to connect to {}", sdkUrl, e);
      if (shutdownOnEdgeFailureConnection) {
        log.warn("Edge connection failed, shutting down");
        featureRepository.notify(SSEResultState.FAILURE, null);
        shutdown();
      }
    }

    eventInput = null; // so shutdown doesn't get confused

    log.debug("connection closed, reconnecting");
    initialized = false;

    if (!shutdown) {
      // timeout should be configurable
      if (System.currentTimeMillis() - start < 2000) {
        executor.execute(this::avoidServerDdos);
      } else {
        // if we have fallen out, try again
        executor.execute(this::listenUntilDead);
      }
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

  /**
   * Tell the client to shutdown when we next fall off.
   */
  public void shutdown() {
    this.shutdown = true;
    if (eventInput != null) {
      try {
        eventInput.close();
      } catch (Exception e) {} // ignore
    }
  }

  public boolean isShutdownOnServerFailure() {
    return shutdownOnServerFailure;
  }

  public void setShutdownOnServerFailure(boolean shutdownOnServerFailure) {
    this.shutdownOnServerFailure = shutdownOnServerFailure;
  }

  public boolean isShutdownOnEdgeFailureConnection() {
    return shutdownOnEdgeFailureConnection;
  }

  public void setShutdownOnEdgeFailureConnection(boolean shutdownOnEdgeFailureConnection) {
    this.shutdownOnEdgeFailureConnection = shutdownOnEdgeFailureConnection;
  }

  // the x-featurehub header has changed, so store it and trigger another run at the server
  @Override
  public void notify(String header) {
    xFeaturehubHeader = header;

    if (initialized) {
      try {
        eventInput.close();
      } catch (Exception ignored) {}
    }
  }
}
