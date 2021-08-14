package io.featurehub;

import cd.connect.jersey.JerseyHttp2Server;
import cd.connect.lifecycle.ApplicationLifecycleManager;
import cd.connect.lifecycle.LifecycleStatus;
import io.featurehub.health.CommonFeatureHubFeatures;
import io.featurehub.health.MetricsHealthRegistration;
import io.featurehub.jersey.config.EndpointLoggingListener;
import io.featurehub.mr.ManagementRepositoryFeature;
import io.featurehub.mr.utils.NginxUtils;
import io.featurehub.publish.NATSFeature;
import io.featurehub.web.security.oauth.OAuth2Feature;
import org.glassfish.jersey.server.ResourceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Application {
  private static final Logger log = LoggerFactory.getLogger(Application.class);


  public static void main(String[] args) {
    System.setProperty("user.timezone", "UTC");
    try {
      // in case we are running in the docker image
      NginxUtils.seeIfWeNeedToRunNginx();
      new Application().run();
    } catch (Exception e) {
      log.error("failed", e);
      System.exit(-1);
    }
  }

  private void run() throws Exception {
    // register our resources, try and tag them as singleton as they are instantiated faster
    ResourceConfig config = new ResourceConfig(
      CommonFeatureHubFeatures.class,
      EndpointLoggingListener.class,
      ManagementRepositoryFeature.class,
      OAuth2Feature.class,
      NATSFeature.class
      );

    MetricsHealthRegistration.Companion.registerMetrics(config);

    new JerseyHttp2Server().start(config);

    log.info("MR Launched - (HTTP/2 payloads enabled!)");

    // tell the App we are ready
    ApplicationLifecycleManager.updateStatus(LifecycleStatus.STARTED);

    Thread.currentThread().join();
  }
}
