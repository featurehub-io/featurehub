package io.featurehub.web.security.saml

import bathe.BatheBooter
import cd.connect.lifecycle.ApplicationLifecycleManager
import cd.connect.lifecycle.LifecycleStatus
import io.featurehub.health.CommonFeatureHubFeatures
import io.featurehub.jersey.FeatureHubJerseyHost
import io.featurehub.lifecycle.TelemetryFeature
import org.glassfish.hk2.api.Immediate
import org.glassfish.jersey.internal.inject.AbstractBinder
import org.glassfish.jersey.server.ResourceConfig
import org.junit.Test
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class SamlTestServer {
  private static final Logger log = LoggerFactory.getLogger(SamlTestServer.class)

  @Test
  void start() {
    new BatheBooter().run(new String[]{"-R" + SamlTestServer.class.getName(),
      "-Pclasspath:/application.properties",
      "-P\${user.home}/.featurehub/saml.properties"});

  }

  static void main(String[] args) {
    try {
      ApplicationLifecycleManager.updateStatus(LifecycleStatus.STARTING)
      def config = new ResourceConfig(SamlResource, CommonFeatureHubFeatures)

      config.register(new AbstractBinder() {
        @Override
        protected void configure() {
          bind(EnvironmentSamlSources).to(SamlConfigSources).in(Immediate)
        }
      })

      new FeatureHubJerseyHost(config).disallowWebHosting().start()
      log.info("SAML Test Server Launched - (HTTP/2 payloads enabled!)")

      Thread.currentThread().join()
    } catch (Exception e) {
      log.error("failed miserably", e)
      System.exit(-1)
    }
  }
}
