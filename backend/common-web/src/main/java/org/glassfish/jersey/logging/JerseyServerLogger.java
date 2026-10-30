package org.glassfish.jersey.logging;

import cd.connect.jersey.common.logging.JerseyFilteringConfiguration;

import jakarta.inject.Singleton;

@Singleton
public class JerseyServerLogger extends FilteringServerLoggingFilter {
	public JerseyServerLogger() {
		super(new JerseyFilteringConfiguration());
	}
}
