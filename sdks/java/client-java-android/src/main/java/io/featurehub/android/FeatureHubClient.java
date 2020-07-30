package io.featurehub.android;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.featurehub.client.FeatureRepository;
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

public class FeatureHubClient {
  private static final Logger log = LoggerFactory.getLogger(FeatureHubClient.class);
  private final FeatureRepository repository;
  private final OkHttpClient client;
  private boolean makeRequests;
  private final String url;
  private final ObjectMapper mapper = new ObjectMapper();

  public FeatureHubClient(String host, Collection<String> sdkUrls, FeatureRepository repository,
                          OkHttpClient client) {
    this.repository = repository;
    this.client = client;

    this.makeRequests = sdkUrls != null && !sdkUrls.isEmpty();
    if (this.makeRequests) {
      url = host + "/features?" + sdkUrls.stream().map(u -> "sdkUrl=" + u).collect(Collectors.joining("&"));
    } else {
      log.error("FeatureHubClient initialized without any sdkUrls");
      url = null;
    }
  }

  public FeatureHubClient(String host, Collection<String> sdkUrls, FeatureRepository repository) {
    this(host, sdkUrls, repository, new OkHttpClient());
  }

  private final static TypeReference<List<Environment>> ref = new TypeReference<List<Environment>>(){};
  private boolean busy = false;

  public void checkForUpdates() {
    if (makeRequests && !busy) {
      busy = true;

      Request request = new Request.Builder().url(url).build();

      client.newCall(request).enqueue(new Callback() {
        @Override
        public void onFailure(Call call,  IOException e) {
          log.error("Unable to call for features", e);
          repository.notify(SSEResultState.FAILURE, null);
          busy = false;
        }

        @Override
        public void onResponse(Call call,  Response response) throws IOException {
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
      });
    }
  }
}
