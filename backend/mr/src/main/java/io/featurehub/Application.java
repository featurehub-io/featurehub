package io.featurehub;

import cd.connect.jersey.JerseyHttp2Server;
import cd.connect.jersey.common.HealthResource;
import cd.connect.jersey.common.InfrastructureConfiguration;
import cd.connect.jersey.common.JerseyPrometheusResource;
import cd.connect.jersey.common.LoggingConfiguration;
import cd.connect.jersey.common.TracingConfiguration;
import cd.connect.jersey.prometheus.PrometheusDynamicFeature;
import cd.connect.lifecycle.ApplicationLifecycleManager;
import cd.connect.lifecycle.LifecycleStatus;
import cd.connect.openapi.support.ReturnStatusContainerResponseFilter;
import io.featurehub.health.HealthFeature;
import io.featurehub.jersey.config.CommonConfiguration;
import io.featurehub.jersey.config.EndpointLoggingListener;
import io.featurehub.mr.ManagementRepositoryFeature;
import io.featurehub.mr.resources.oauth2.OAuth2Feature;
import io.featurehub.mr.utils.NginxUtils;
import io.opentracing.contrib.jaxrs2.client.ClientTracingFeature;
import io.prometheus.client.hotspot.DefaultExports;
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

    // turn on all jvm prometheus metrics
    DefaultExports.initialize();

    // register our resources, try and tag them as singleton as they are instantiated faster
    ResourceConfig config = new ResourceConfig(
      ClientTracingFeature.class,
      CommonConfiguration.class,
      LoggingConfiguration.class,
      ReturnStatusContainerResponseFilter.class,
      TracingConfiguration.class
      )
      .register(EndpointLoggingListener.class)
      .register(ManagementRepositoryFeature.class)
      .register(HealthFeature.class)
      .register(OAuth2Feature.class);

    new JerseyHttp2Server().start(config);

    log.info("MR Launched - (HTTP/2 payloads enabled!)");

    // tell the App we are ready
    ApplicationLifecycleManager.updateStatus(LifecycleStatus.STARTED);

    Thread.currentThread().join();
  }
}
