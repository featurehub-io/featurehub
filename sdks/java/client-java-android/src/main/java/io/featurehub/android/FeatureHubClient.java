package io.featurehub.android;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.featurehub.client.EdgeService;
import io.featurehub.client.FeatureHubConfig;
import io.featurehub.client.FeatureStore;
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

  public FeatureHubClient(String host, Collection<String> sdkUrls, FeatureStore repository,
                          Call.Factory client, FeatureHubConfig config) {
    this.repository = repository;
    this.client = client;
    this.config = config;

    if (host != null && sdkUrls != null && !sdkUrls.isEmpty()) {
      this.clientSideEvaluation = sdkUrls.stream().anyMatch(FeatureHubConfig::sdkKeyIsClientSideEvaluated);

      this.makeRequests = true;

      url = host + "/features?" + sdkUrls.stream().map(u -> "sdkUrl=" + u).collect(Collectors.joining("&"));
    } else {
      log.error("FeatureHubClient initialized without any sdkUrls");
      url = null;
      this.clientSideEvaluation = false;
    }
  }

  public FeatureHubClient(String host, Collection<String> sdkUrls, FeatureStore repository, FeatureHubConfig config) {
    this(host, sdkUrls, repository, new OkHttpClient(), config);
  }

  private final static TypeReference<List<Environment>> ref = new TypeReference<List<Environment>>(){};
  private boolean busy = false;

  public void checkForUpdates() {
    if (makeRequests && !busy) {
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
  }

  protected void processFailure(IOException e) {
    log.error("Unable to call for features", e);
    repository.notify(SSEResultState.FAILURE, null);
    busy = false;
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

  @Override
  public void contextChange(String newHeader) {
    if (!newHeader.equals(xFeaturehubHeader)) {
      xFeaturehubHeader = newHeader;
      checkForUpdates();
    }
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
