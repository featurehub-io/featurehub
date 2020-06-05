package io.featurehub.mr;

import cd.connect.jersey.common.CorsFilter;
import cd.connect.jersey.common.InfrastructureConfiguration;
import cd.connect.jersey.common.LoggingConfiguration;
import cd.connect.jersey.common.TracingConfiguration;
import io.featurehub.db.publish.CacheSource;
import io.featurehub.db.publish.MRPublishModule;
import io.featurehub.db.publish.PublishManager;
import io.featurehub.db.utils.MRSqlModule;
import io.featurehub.mr.auth.AuthApplicationEventListener;
import io.featurehub.mr.auth.AuthManager;
import io.featurehub.mr.auth.AuthManagerService;
import io.featurehub.mr.auth.AuthenticationRepository;
import io.featurehub.mr.auth.DatabaseAuthRepository;
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
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.jersey.internal.inject.AbstractBinder;
import org.glassfish.jersey.server.ServerProperties;
import org.glassfish.jersey.server.spi.Container;
import org.glassfish.jersey.server.spi.ContainerLifecycleListener;
import org.glassfish.jersey.server.validation.ValidationFeature;

import javax.inject.Singleton;
import javax.ws.rs.core.Feature;
import javax.ws.rs.core.FeatureContext;
import java.util.Arrays;

public class ManagementRepositoryFeature implements Feature {
  @Override
  public boolean configure(FeatureContext context) {
    Arrays.asList(ValidationFeature.class,
      InitializeResource.class,
      PortfolioResource.class,
      LogoutResource.class,
      PersonResource.class,
      AuthenticationResource.class,
      GroupResource.class,
      LoginResource.class,
      ServiceAccountResource.class,
      ApplicationResource.class,
      EnvironmentResource.class,
      FeatureResource.class,
      CorsFilter.class,
      ConstraintExceptionHandler.class,
      AuthApplicationEventListener.class
      ).forEach(o -> context.register(o));

    context.register(new MRSqlModule());
    context.register(new MRPublishModule());
    context.property(ServerProperties.BV_SEND_ERROR_IN_RESPONSE, true);
    context.register(new AbstractBinder() {
        @Override
        protected void configure() {
          bind(DatabaseAuthRepository.class).to(AuthenticationRepository.class).in(Singleton.class);
          bind(PortfolioUtils.class).to(PortfolioUtils.class).in(Singleton.class);
          bind(AuthManager.class).to(AuthManagerService.class).in(Singleton.class);
        }
      }).register(new ContainerLifecycleListener() {
        public void onStartup(Container container) {
          // access the ServiceLocator here
          ServiceLocator injector = container.getApplicationHandler()
            .getInjectionManager().getInstance(ServiceLocator.class);

          injector.getService(CacheSource.class);
          injector.getService(PublishManager.class);
        }

        public void onReload(Container container) {
        }

        public void onShutdown(Container container) {/*...*/}
      });

    return true;
  }
}
