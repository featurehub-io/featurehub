package io.featurehub.client.jersey;

import cd.connect.openapi.support.ApiClient;
import io.featurehub.client.EdgeService;
import io.featurehub.client.Feature;
import io.featurehub.client.FeatureHubConfig;
import io.featurehub.client.FeatureStateUtils;
import io.featurehub.client.FeatureStore;
import io.featurehub.sse.api.FeatureService;
import io.featurehub.sse.model.FeatureStateUpdate;
import io.featurehub.sse.model.SSEResultState;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.media.sse.EventInput;
import org.glassfish.jersey.media.sse.InboundEvent;
import org.glassfish.jersey.media.sse.SseFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Singleton
public class JerseyClient implements EdgeService {
  private static final Logger log = LoggerFactory.getLogger(JerseyClient.class);
  private final WebTarget target;
  private boolean initialized;
  private final Executor executor;
  private final FeatureStore featureRepository;
  private final FeatureService featuresService;
  private boolean shutdown = false;
  private boolean shutdownOnServerFailure = true;
  private boolean shutdownOnEdgeFailureConnection = false;
  private EventInput eventInput;
  private String xFeaturehubHeader;
  protected final FeatureHubConfig fhConfig;

  public JerseyClient(FeatureHubConfig config, FeatureStore repository) {
    this(config, !config.isServerEvaluation(), repository, null);
  }

  public JerseyClient(FeatureHubConfig config, boolean initializeOnConstruction,
                      FeatureStore featureRepository, ApiClient apiClient) {
    this.featureRepository = featureRepository;
    this.fhConfig = config;

    log.info("new jersey client created");

    featureRepository.setServerEvaluation(config.isServerEvaluation());

    Client client = ClientBuilder.newBuilder()
      .register(JacksonFeature.class)
      .register(SseFeature.class).build();

    target = makeEventSourceTarget(client, config.getRealtimeUrl());
    executor = makeExecutor();

    if (apiClient == null) {
      apiClient = new ApiClient(client, config.baseUrl());
    }

    featuresService = makeFeatureServiceClient(apiClient);

    if (initializeOnConstruction) {
      init();
    }
  }

  protected Executor makeExecutor() {
    // in case they keep changing the context, it will ask the server and cancel and ask and cancel
    // if they are in client mode
    return Executors.newFixedThreadPool(4);
  }

  protected WebTarget makeEventSourceTarget(Client client, String sdkUrl) {
    return client.target(sdkUrl);
  }

  protected FeatureService makeFeatureServiceClient(ApiClient apiClient) {
    return new FeatureServiceImpl(apiClient);
  }

  public void setFeatureState(String key, FeatureStateUpdate update) {
    featuresService.setFeatureState(fhConfig.sdkKey(), key, update);
  }

  public void setFeatureState(Feature feature, FeatureStateUpdate update) {
    setFeatureState(feature.name(), update);
  }

  // backoff algorithm should be configurable
  private void avoidServerDdos() {
    if (request != null) {
      request.active = false;
      request = null;
    }

    try {
      Thread.sleep(10000); // wait 10 seconds
    } catch (InterruptedException e) {
    }

    if (!shutdown) {
      executor.execute(this::restartRequest);
    }
  }

  private CurrentRequest request;

  class CurrentRequest {
    public boolean active = true;

    public void listenUntilDead() {
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

          // we cannot force close the client input, it hangs around and waits for the server
          if (!active) {
            log.info("not active");
            return; // ignore all data from this call, it is no longer active or relevant
          }

          if (shutdown || inboundEvent == null) { // connection has been closed or is shutdown
            log.info("shutdown?");
            break;
          }

          log.info("notifying of {}", inboundEvent.getName());

          final SSEResultState state = SSEResultState.fromValue(inboundEvent.getName());
          featureRepository.notify(state, inboundEvent.readData());

          if (state == SSEResultState.FAILURE && shutdownOnServerFailure) {
            log.warn("Failed to connect to FeatureHub Edge, shutting down.");
            shutdown();
          }
        }
      } catch (Exception e) {
        if (shutdownOnEdgeFailureConnection) {
          log.warn("Edge connection failed, shutting down");
          featureRepository.notify(SSEResultState.FAILURE, null);
          shutdown();
        }
      }

      eventInput = null; // so shutdown doesn't get confused

      initialized = false;

      if (!shutdown) {
        log.debug("connection closed, reconnecting");
        // timeout should be configurable
        if (System.currentTimeMillis() - start < 2000) {
          executor.execute(JerseyClient.this::avoidServerDdos);
        } else {
          // if we have fallen out, try again
          executor.execute(this::listenUntilDead);
        }
      } else {
        log.info("featurehub client shut down");
      }
    }
  }

  public boolean isInitialized() {
    return initialized;
  }

  private void restartRequest() {
    log.info("starting new request");
    if (request != null) {
      request.active = false;
    }

    initialized = false;

    request = new CurrentRequest();
    request.listenUntilDead();
  }

  void init() {
    if (!initialized) {
      executor.execute(this::restartRequest);
    }
  }

  /**
   * Tell the client to shutdown when we next fall off.
   */
  public void shutdown() {
    log.info("starting shutdown of jersey edge client");
    this.shutdown = true;

    if (request != null) {
      request.active = false;
    }

    if (executor instanceof ExecutorService) {
      ((ExecutorService)executor).shutdownNow();
    }

    log.info("exiting shutdown of jersey edge client");
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

  public String getFeaturehubContextHeader() {
    return xFeaturehubHeader;
  }

  @Override
  public void contextChange(String header) {
    if (!header.equals(xFeaturehubHeader) || !initialized) {
      xFeaturehubHeader = header;

      executor.execute(this::restartRequest);
    }
  }

  @Override
  public boolean isClientEvaluation() {
    return !fhConfig.isServerEvaluation();
  }

  @Override
  public void close() {
    shutdown();
  }

  @Override
  public FeatureHubConfig getConfig() {
    return fhConfig;
  }

  @Override
  public boolean isRequiresReplacementOnHeaderChange() {
    return true;
  }

  @Override
  public void poll() {
    // do nothing, its SSE
  }
}
