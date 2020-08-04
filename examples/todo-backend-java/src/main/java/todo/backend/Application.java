package todo.backend;

import cd.connect.app.config.ConfigKey;
import cd.connect.app.config.DeclaredConfigResolver;
import cd.connect.jersey.common.*;
import cd.connect.lifecycle.ApplicationLifecycleManager;
import cd.connect.lifecycle.LifecycleStatus;
import io.featurehub.client.ClientFeatureRepository;
import io.featurehub.client.FeatureRepository;
import io.featurehub.client.GoogleAnalyticsCollector;
import io.featurehub.client.Readyness;
import io.featurehub.client.StaticFeatureContext;
import io.featurehub.client.interceptor.OpenTracingValueInterceptor;
import io.featurehub.client.interceptor.SystemPropertyValueInterceptor;
import io.featurehub.client.jersey.GoogleAnalyticsJerseyApiClient;
import io.featurehub.client.jersey.JerseyClient;
import io.opentracing.contrib.jaxrs2.client.ClientTracingFeature;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import todo.backend.resources.FeatureAnalyticsFilter;
import todo.backend.resources.TodoResource;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.TimeUnit;

public class Application {
	private static final Logger log = LoggerFactory.getLogger(Application.class);
	@ConfigKey("server.port")
	String serverPort = "8099";
	@ConfigKey("feature-service.url")
  String featureHubUrl;
	@ConfigKey("feature-service.google-analytics-key")
  String analyticsKey;
	@ConfigKey("feature-service.cid")
  String analyticsCid;

  public Application() {
    DeclaredConfigResolver.resolve(this);
  }

  public void init() throws Exception {

    FeatureRepository cfr = new ClientFeatureRepository(5);
    cfr.registerValueInterceptor(new SystemPropertyValueInterceptor());
    cfr.registerValueInterceptor(new OpenTracingValueInterceptor());
    cfr.addAnalyticCollector(new GoogleAnalyticsCollector(analyticsKey, analyticsCid, new GoogleAnalyticsJerseyApiClient()));

    StaticFeatureContext.repository = cfr;
//    new JerseyClient(featureHubUrl, true, cfr);

    URI BASE_URI = URI.create(String.format("http://0.0.0.0:%s/", serverPort));

    log.info("attemping to start on port {} - will wait for features", BASE_URI.toASCIIString());

    // register our resources, try and tag them as singleton as they are instantiated faster
    ResourceConfig config = new ResourceConfig(
      TodoResource.class,
      CorsFilter.class,
      ClientTracingFeature.class,
      CommonConfiguration.class,
      LoggingConfiguration.class,
      TracingConfiguration.class,
      FeatureAnalyticsFilter.class, // why not
      InfrastructureConfiguration.class);

    final HttpServer server = GrizzlyHttpServerFactory.createHttpServer(BASE_URI, config, false);

    server.start();

    cfr.addReadynessListener((ready) -> {
      if (ready == Readyness.Ready) {
        try {
          server.start();
        } catch (IOException e) {
          log.error("Failed to start", e);
          throw new RuntimeException(e);
        }

        log.info("Application started. (HTTP/2 enabled!) -> {}", BASE_URI);
      } else if (ready == Readyness.Failed) {
        server.shutdownNow(); // probably should wait
        log.error("Connect to feature server failed, shutting down server.");
        System.exit(-1);
      }
    });

    ApplicationLifecycleManager.registerListener(trans -> {
      if (trans.next == LifecycleStatus.TERMINATING) {
        server.shutdown(10, TimeUnit.SECONDS);
      }
    });

    // tell the App we are ready
    ApplicationLifecycleManager.updateStatus(LifecycleStatus.STARTED);

    Thread.currentThread().join();
  }

  public static void main(String[] args) throws Exception {
    new Application().init();
	}


}
