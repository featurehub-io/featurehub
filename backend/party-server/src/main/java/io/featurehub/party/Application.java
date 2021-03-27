package io.featurehub.party;

import cd.connect.jersey.JerseyHttp2Server;
import cd.connect.jersey.common.CorsFilter;
import cd.connect.jersey.common.InfrastructureConfiguration;
import cd.connect.jersey.common.LoggingConfiguration;
import cd.connect.jersey.common.TracingConfiguration;
import cd.connect.lifecycle.ApplicationLifecycleManager;
import cd.connect.lifecycle.LifecycleStatus;
import cd.connect.openapi.support.ReturnStatusContainerResponseFilter;
import io.featurehub.edge.EdgeFeature;
import io.featurehub.jersey.config.CommonConfiguration;
import io.featurehub.mr.ManagementRepositoryFeature;
import io.featurehub.mr.utils.NginxUtils;
import io.opentracing.contrib.jaxrs2.client.ClientTracingFeature;
import io.prometheus.client.hotspot.DefaultExports;
import org.glassfish.jersey.server.ResourceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Application {
  private static final Logger log = LoggerFactory.getLogger(io.featurehub.Application.class);

  public static void main(String[] args) {
    System.setProperty("user.timezone", "UTC");
    try {
      NginxUtils.seeIfWeNeedToRunNginx();
      new io.featurehub.party.Application().run();
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
      TracingConfiguration.class,
      ReturnStatusContainerResponseFilter.class,
      InfrastructureConfiguration.class)
      .register(CorsFilter.class)
      .register(ManagementRepositoryFeature.class)
      .register(EdgeFeature.class);

    new JerseyHttp2Server().start(config);

    // start the dacha layer
    io.featurehub.dacha.Application.initializeDacha();

    log.info("MR Launched - (HTTP/2 payloads enabled!)");

    // tell the App we are ready
    ApplicationLifecycleManager.updateStatus(LifecycleStatus.STARTED);

    Thread.currentThread().join();
  }
}
