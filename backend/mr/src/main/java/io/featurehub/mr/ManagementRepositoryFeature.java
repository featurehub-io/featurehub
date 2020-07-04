package io.featurehub.mr;

import cd.connect.jersey.common.CorsFilter;
import io.featurehub.db.publish.CacheSource;
import io.featurehub.db.publish.MRPublishModule;
import io.featurehub.db.publish.PublishManager;
import io.featurehub.db.utils.MROrgBindingModule;
import io.featurehub.db.utils.MRSqlModule;
import io.featurehub.mr.api.ApplicationServiceDelegate;
import io.featurehub.mr.api.ApplicationServiceDelegator;
import io.featurehub.mr.api.AuthServiceDelegate;
import io.featurehub.mr.api.AuthServiceDelegator;
import io.featurehub.mr.api.EnvironmentFeatureServiceDelegate;
import io.featurehub.mr.api.EnvironmentFeatureServiceDelegator;
import io.featurehub.mr.api.EnvironmentServiceDelegate;
import io.featurehub.mr.api.EnvironmentServiceDelegator;
import io.featurehub.mr.api.FeatureServiceDelegate;
import io.featurehub.mr.api.FeatureServiceDelegator;
import io.featurehub.mr.api.GroupServiceDelegate;
import io.featurehub.mr.api.GroupServiceDelegator;
import io.featurehub.mr.api.PersonServiceDelegate;
import io.featurehub.mr.api.PersonServiceDelegator;
import io.featurehub.mr.api.PortfolioServiceDelegate;
import io.featurehub.mr.api.PortfolioServiceDelegator;
import io.featurehub.mr.api.ServiceAccountServiceDelegate;
import io.featurehub.mr.api.ServiceAccountServiceDelegator;
import io.featurehub.mr.api.SetupServiceDelegate;
import io.featurehub.mr.api.SetupServiceDelegator;
import io.featurehub.mr.auth.AuthApplicationEventListener;
import io.featurehub.mr.auth.AuthManager;
import io.featurehub.mr.auth.AuthManagerService;
import io.featurehub.mr.auth.AuthenticationRepository;
import io.featurehub.mr.auth.DatabaseAuthRepository;
import io.featurehub.mr.resources.ApplicationResource;
import io.featurehub.mr.resources.AuthResource;
import io.featurehub.mr.resources.EnvironmentFeatureResource;
import io.featurehub.mr.resources.EnvironmentResource;
import io.featurehub.mr.resources.FeatureResource;
import io.featurehub.mr.resources.GroupResource;
import io.featurehub.mr.resources.PersonResource;
import io.featurehub.mr.resources.PortfolioResource;
import io.featurehub.mr.resources.ServiceAccountResource;
import io.featurehub.mr.resources.SetupResource;
import io.featurehub.mr.utils.ApplicationUtils;
import io.featurehub.mr.utils.PortfolioUtils;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.jersey.internal.inject.AbstractBinder;
import org.glassfish.jersey.server.ServerProperties;
import org.glassfish.jersey.server.spi.Container;
import org.glassfish.jersey.server.spi.ContainerLifecycleListener;

import javax.inject.Singleton;
import javax.ws.rs.core.Feature;
import javax.ws.rs.core.FeatureContext;
import java.util.Arrays;

public class ManagementRepositoryFeature implements Feature {
  @Override
  public boolean configure(FeatureContext context) {
    Arrays.asList(
      //ValidationFeature.class,
      ApplicationServiceDelegator.class,
      AuthServiceDelegator.class,
      EnvironmentFeatureServiceDelegator.class,
      EnvironmentServiceDelegator.class,
      FeatureServiceDelegator.class,
      GroupServiceDelegator.class,
      PersonServiceDelegator.class,
      PortfolioServiceDelegator.class,
      ServiceAccountServiceDelegator.class,
      SetupServiceDelegator.class,
      CorsFilter.class,
//      ConstraintExceptionHandler.class,
      AuthApplicationEventListener.class
      ).forEach(o -> context.register(o));

    context.register(new MRSqlModule());
    context.register(new MROrgBindingModule());
    context.register(new MRPublishModule());
    context.property(ServerProperties.BV_SEND_ERROR_IN_RESPONSE, true);
    context.register(new AbstractBinder() {
        @Override
        protected void configure() {
          bind(DatabaseAuthRepository.class).to(AuthenticationRepository.class).in(Singleton.class);
          bind(PortfolioUtils.class).to(PortfolioUtils.class).in(Singleton.class);
          bind(AuthManager.class).to(AuthManagerService.class).in(Singleton.class);
          bind(ApplicationResource.class).to(ApplicationServiceDelegate.class).in(Singleton.class);
          bind(AuthResource.class).to(AuthServiceDelegate.class).in(Singleton.class);
          bind(EnvironmentFeatureResource.class).to(EnvironmentFeatureServiceDelegate.class).in(Singleton.class);
          bind(EnvironmentResource.class).to(EnvironmentServiceDelegate.class).in(Singleton.class);
          bind(FeatureResource.class).to(FeatureServiceDelegate.class).in(Singleton.class);
          bind(GroupResource.class).to(GroupServiceDelegate.class).in(Singleton.class);
          bind(PersonResource.class).to(PersonServiceDelegate.class).in(Singleton.class);
          bind(PortfolioResource.class).to(PortfolioServiceDelegate.class).in(Singleton.class);
          bind(ServiceAccountResource.class).to(ServiceAccountServiceDelegate.class).in(Singleton.class);
          bind(SetupResource.class).to(SetupServiceDelegate.class).in(Singleton.class);
          bind(ApplicationUtils.class).to(ApplicationUtils.class).in(Singleton.class);
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
