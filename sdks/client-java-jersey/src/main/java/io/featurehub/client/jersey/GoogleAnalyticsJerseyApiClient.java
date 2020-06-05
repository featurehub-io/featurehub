package io.featurehub.client.jersey;

import cd.connect.jersey.common.LoggingConfiguration;
import io.featurehub.client.GoogleAnalyticsApiClient;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;

public class GoogleAnalyticsJerseyApiClient implements GoogleAnalyticsApiClient {
  private final WebTarget target;

  public GoogleAnalyticsJerseyApiClient() {
    target = ClientBuilder.newBuilder()
      .register(LoggingConfiguration.class)
      .build().target("https://www.google-analytics.com/batch");
  }

  @Override
  public void postBatchUpdate(String batchData) {
    target.request().header("Host", "www.google-analytics.com").post(Entity.entity(batchData, MediaType.APPLICATION_FORM_URLENCODED_TYPE));
  }
}
