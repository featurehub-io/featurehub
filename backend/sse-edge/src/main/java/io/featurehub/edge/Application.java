package io.featurehub.edge;

import cd.connect.app.config.ConfigKey;
import cd.connect.app.config.DeclaredConfigResolver;
import cd.connect.jersey.JerseyHttp2Server;
import cd.connect.jersey.common.CorsFilter;
import cd.connect.lifecycle.ApplicationLifecycleManager;
import cd.connect.lifecycle.LifecycleStatus;
import io.featurehub.dacha.api.DachaClientFeature;
import io.featurehub.health.CommonFeatureHubFeatures;
import io.featurehub.health.MetricsHealthRegistration;
import io.featurehub.jersey.config.EndpointLoggingListener;
import io.featurehub.jersey.config.LocalExceptionMapper;
import io.featurehub.publish.NATSFeature;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.http.server.NetworkListener;
import org.glassfish.grizzly.http2.Http2AddOn;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class Application {
  @ConfigKey("server.port")
  String serverPort = "8553";

  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(Application.class);

  public void run() throws Exception {
    ApplicationLifecycleManager.updateStatus(LifecycleStatus.STARTING);

    DeclaredConfigResolver.resolve(this);

    URI BASE_URI = URI.create(String.format("http://0.0.0.0:%s/", serverPort));

    log.info("starting on port {}", BASE_URI.toASCIIString());

    ResourceConfig config = new ResourceConfig(
      LocalExceptionMapper.class,
      EndpointLoggingListener.class,
      NATSFeature.class,
      CommonFeatureHubFeatures.class,
      DachaClientFeature.class,
      EdgeFeature.class,
      EdgeResourceFeature.class,
      CorsFilter.class
      );

    // check if we should list on a different port
    MetricsHealthRegistration.Companion.registerMetrics(config);

    // this has a default grace period of 10 seconds
    JerseyHttp2Server.start(config, BASE_URI);

    ApplicationLifecycleManager.updateStatus(LifecycleStatus.STARTED);

    log.info("FeatureHub SSE-Edge Has Started.");

    Thread.currentThread().join();
  }

  public static void main(String[] args) {
    try {
      new Application().run();
    } catch (Exception e) {
      log.error("Failed to start.", e);
      ApplicationLifecycleManager.updateStatus(LifecycleStatus.TERMINATED);
      System.exit(-1);
    }
  }
}
