package io.featurehub.edge;

import cd.connect.app.config.ConfigKey;
import cd.connect.app.config.DeclaredConfigResolver;
import cd.connect.jersey.JerseyHttp2Server;
import cd.connect.jersey.common.CommonConfiguration;
import cd.connect.jersey.common.CorsFilter;
import cd.connect.jersey.common.InfrastructureConfiguration;
import cd.connect.jersey.common.LoggingConfiguration;
import cd.connect.lifecycle.ApplicationLifecycleManager;
import cd.connect.lifecycle.LifecycleStatus;
import io.featurehub.edge.rest.SSEHeaderFilter;
import io.prometheus.client.hotspot.DefaultExports;
import org.glassfish.jersey.server.ResourceConfig;

import javax.ws.rs.container.ContainerResponseFilter;
import java.net.URI;

public class Application {
  @ConfigKey("server.port")
  String serverPort = "8553";

  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(Application.class);

  public Application() {
  }

  public void run() throws Exception {
    ApplicationLifecycleManager.updateStatus(LifecycleStatus.STARTING);

    DeclaredConfigResolver.resolve(this);

    // turn on all jvm prometheus metrics
    DefaultExports.initialize();

    URI BASE_URI = URI.create(String.format("http://0.0.0.0:%s/", serverPort));

    log.info("starting on port {}", BASE_URI.toASCIIString());

    ResourceConfig config = new ResourceConfig()
      .register(InfrastructureConfiguration.class)
      .register(CommonConfiguration.class)
      .register(CorsFilter.class)
      .register(LoggingConfiguration.class)
      .register(EdgeFeature.class)
      ;

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
      log.error("Failed to start.");
      System.exit(-1);
    }
  }
}
