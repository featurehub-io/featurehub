package io.featurehub;

import cd.connect.jersey.JerseyHttp2Server;
import cd.connect.jersey.common.CorsFilter;
import cd.connect.jersey.common.InfrastructureConfiguration;
import cd.connect.jersey.common.LoggingConfiguration;
import cd.connect.jersey.common.TracingConfiguration;
import cd.connect.lifecycle.ApplicationLifecycleManager;
import cd.connect.lifecycle.LifecycleStatus;
import io.featurehub.db.publish.CacheSource;
import io.featurehub.db.publish.MRPublishModule;
import io.featurehub.db.publish.PublishManager;
import io.featurehub.db.utils.MRSqlModule;
import io.featurehub.mr.ManagementRepositoryFeature;
import io.featurehub.mr.auth.AuthApplicationEventListener;
import io.featurehub.mr.auth.AuthManager;
import io.featurehub.mr.auth.AuthManagerService;
import io.featurehub.mr.auth.AuthenticationRepository;
import io.featurehub.mr.auth.DatabaseAuthRepository;
import io.featurehub.mr.auth.InMemoryAuthRepository;
import io.featurehub.mr.filters.CommonConfiguration;
import io.featurehub.mr.filters.ConstraintExceptionHandler;
import io.featurehub.mr.rest.ApplicationResource;
import io.featurehub.mr.rest.AuthenticationResource;
import io.featurehub.mr.rest.EnvironmentResource;
import io.featurehub.mr.rest.FeatureResource;
import io.featurehub.mr.rest.GroupResource;
import io.featurehub.mr.rest.InitializeResource;
import io.featurehub.mr.rest.LoginResource;
import io.featurehub.mr.rest.LogoutResource;
import io.featurehub.mr.rest.PersonResource;
import io.featurehub.mr.rest.PortfolioResource;
import io.featurehub.mr.rest.ServiceAccountResource;
import io.featurehub.mr.utils.PortfolioUtils;
import io.opentracing.contrib.jaxrs2.client.ClientTracingFeature;
import io.prometheus.client.hotspot.DefaultExports;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.jersey.internal.inject.AbstractBinder;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ServerProperties;
import org.glassfish.jersey.server.spi.Container;
import org.glassfish.jersey.server.spi.ContainerLifecycleListener;
import org.glassfish.jersey.server.validation.ValidationFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;

public class Application {
  private static final Logger log = LoggerFactory.getLogger(Application.class);


  public static void main(String[] args) {
    System.setProperty("user.timezone", "UTC");
    try {
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
      TracingConfiguration.class,
      InfrastructureConfiguration.class).register(ManagementRepositoryFeature.class);

    new JerseyHttp2Server().start(config);

    log.info("MR Launched - (HTTP/2 payloads enabled!)");

    // tell the App we are ready
    ApplicationLifecycleManager.updateStatus(LifecycleStatus.STARTED);

    Thread.currentThread().join();
  }
}
