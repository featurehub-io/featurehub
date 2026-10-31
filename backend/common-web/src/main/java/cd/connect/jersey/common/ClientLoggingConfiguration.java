package cd.connect.jersey.common;

import cd.connect.jersey.common.logging.JerseyFiltering;
import cd.connect.jersey.common.logging.JerseyFilteringConfiguration;
import jakarta.inject.Singleton;
import jakarta.ws.rs.core.Feature;
import jakarta.ws.rs.core.FeatureContext;
import org.glassfish.jersey.internal.inject.AbstractBinder;
import org.glassfish.jersey.logging.FilteringClientLoggingFilter;

public class ClientLoggingConfiguration implements Feature {

	@Override
	public boolean configure(FeatureContext ctx) {
		ctx.register(FilteringClientLoggingFilter.class);
		ctx.register(new AbstractBinder() {
      @Override
      protected void configure() {
        bind(new JerseyFilteringConfiguration()).in(Singleton.class)
          .to(JerseyFiltering.class);
      }
    });
		return true;
	}
}
