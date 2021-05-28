package io.featurehub.android;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.featurehub.client.EdgeService;
import io.featurehub.client.FeatureHubConfig;
import io.featurehub.client.FeatureStore;
import io.featurehub.client.Readyness;
import io.featurehub.sse.model.Environment;
import io.featurehub.sse.model.FeatureState;
import io.featurehub.sse.model.SSEResultState;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

public class FeatureHubClient implements EdgeService {
  private static final Logger log = LoggerFactory.getLogger(FeatureHubClient.class);
  private final FeatureStore repository;
  private final Call.Factory client;
  private boolean makeRequests;
  private final String url;
  private final ObjectMapper mapper = new ObjectMapper();
  private String xFeaturehubHeader;
  private final boolean clientSideEvaluation;
  private final FeatureHubConfig config;
  private final ExecutorService executorService;

  public FeatureHubClient(String host, Collection<String> sdkUrls, FeatureStore repository,
                          Call.Factory client, FeatureHubConfig config) {
    this.repository = repository;
    this.client = client;
    this.config = config;

    if (host != null && sdkUrls != null && !sdkUrls.isEmpty()) {
      this.clientSideEvaluation = sdkUrls.stream().anyMatch(FeatureHubConfig::sdkKeyIsClientSideEvaluated);

      this.makeRequests = true;

      executorService = makeExecutorService();

      url = host + "/features?" + sdkUrls.stream().map(u -> "sdkUrl=" + u).collect(Collectors.joining("&"));

      if (clientSideEvaluation) {
        checkForUpdates();
      }
    } else {
      throw new RuntimeException("FeatureHubClient initialized without any sdkUrls");
    }
  }

  protected ExecutorService makeExecutorService() {
    return Executors.newWorkStealingPool();
  }

  public FeatureHubClient(String host, Collection<String> sdkUrls, FeatureStore repository, FeatureHubConfig config) {
    this(host, sdkUrls, repository, (Call.Factory) new OkHttpClient(), config);
  }

  private final static TypeReference<List<Environment>> ref = new TypeReference<List<Environment>>(){};
  private boolean busy = false;
  private List<CompletableFuture<Readyness>> waitingClients = new ArrayList<>();

  public boolean checkForUpdates() {
    final boolean ask = makeRequests && !busy;

    if (ask) {
      busy = true;

      Request.Builder reqBuilder = new Request.Builder().url(this.url);

      if (xFeaturehubHeader != null) {
        reqBuilder = reqBuilder.addHeader("x-featurehub", xFeaturehubHeader);
      }

      Request request = reqBuilder.build();

      Call call = client.newCall(request);
      call.enqueue(new Callback() {
        @Override
        public void onFailure(Call call,  IOException e) {
          processFailure(e);
        }

        @Override
        public void onResponse(Call call,  Response response) throws IOException {
          processResponse(response);
        }
      });
    }

    return ask;
  }

  protected void processFailure(IOException e) {
    log.error("Unable to call for features", e);
    repository.notify(SSEResultState.FAILURE, null);
    busy = false;
    completeReadyness();
  }

  protected void processResponse(Response response) throws IOException {
    busy = false;

    try (ResponseBody body = response.body()) {
      if (response.isSuccessful() && body != null) {
        List<Environment> environments = mapper.readValue(body.bytes(), ref);
        log.debug("updating feature repository: {}", environments);

        List<FeatureState> states = new ArrayList<>();
        environments.forEach(e -> {
          e.getFeatures().forEach(f -> f.setEnvironmentId(e.getId()));
          states.addAll(e.getFeatures());
        });

        repository.notify(states);
        completeReadyness();
      } else if (response.code() == 400) {
        makeRequests = false;
        log.error("Server indicated an error with our requests making future ones pointless.");
        repository.notify(SSEResultState.FAILURE, null);
      }
    }
  }

  boolean canMakeRequests() {
    return makeRequests;
  }

  private void completeReadyness() {
    List<CompletableFuture<Readyness>> current = waitingClients;
    waitingClients = new ArrayList<>();
    current.forEach(c -> {
      try {
        c.complete(repository.getReadyness());
      } catch (Exception e) {
        log.error("Unable to complete future", e);
      }
    });
  }

  @Override
  public Future<Readyness> contextChange(String newHeader) {
    final CompletableFuture<Readyness> change = new CompletableFuture<>();

    if (!newHeader.equals(xFeaturehubHeader)) {
      xFeaturehubHeader = newHeader;
      if (checkForUpdates() || busy) {
        waitingClients.add(change);
      } else {
        change.complete(repository.getReadyness());
      }
    } else {
      change.complete(repository.getReadyness());
    }

    return change;
  }

  @Override
  public boolean isClientEvaluation() {
    return clientSideEvaluation;
  }

  @Override
  public void close() {
    log.info("featurehub client closed.");

    makeRequests = false;

    if (client instanceof OkHttpClient) {
      ((OkHttpClient)client).dispatcher().executorService().shutdownNow();
    }

    executorService.shutdownNow();
  }

  @Override
  public FeatureHubConfig getConfig() {
    return config;
  }

  @Override
  public boolean isRequiresReplacementOnHeaderChange() {
    return false;
  }

  @Override
  public void poll() {
    checkForUpdates();
  }
}
