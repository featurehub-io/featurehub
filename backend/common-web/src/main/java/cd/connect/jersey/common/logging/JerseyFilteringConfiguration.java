package cd.connect.jersey.common.logging;

import cd.connect.app.config.ConfigKey;
import cd.connect.app.config.DeclaredConfigResolver;
import cd.connect.context.ConnectContext;
import org.glassfish.jersey.logging.Constants;
import org.glassfish.jersey.logging.LoggingFeature;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Being here and in a component allows for it to be reloaded by the configuration system as necessary.
 *
 * Many of these things cannot be reloaded however. Once a ResourceConfig is loaded, it stays loaded. We may
 * support servlet un/reloading later.
 *
 * These are only GLOBAL settings for all jersey contexts. If you load multiple of them and you want individual
 * configuration you will need to do this another way.
 *
 *
 */

public class JerseyFilteringConfiguration implements JerseyFiltering {
  private Set<String> excludeUri;
  private Set<String> excludeEntirelyUri;

  @ConfigKey("jersey.exclude")
  protected String exclude = ""; // e.g. /(apibrowser|metrics|service|status).*
  @ConfigKey("jersey.tracing")
  protected String tracing = ""; // e.g. ON_DEMAND
  @ConfigKey("jersey.bufferSize")
  protected Integer bufferSize = 8092;

  @ConfigKey("jersey.logging.exclude-body-uris")
  protected String excludeBodyUris = "";

  @ConfigKey("jersey.logging.exclude-entirely-uris")
  protected String excludeEntirelyUris = "";

  @ConfigKey("jersey.logging.verbosity")
  protected String verbosity = LoggingFeature.Verbosity.PAYLOAD_ANY.name();

  public JerseyFilteringConfiguration() {
    DeclaredConfigResolver.resolve(this);
    /*
     * we are default using Kubernetes and Prometheus, so we should ignore at least these by default.
     */
    excludeUri = deconstructConfiguration(excludeBodyUris);
    excludeEntirelyUri = deconstructConfiguration(excludeEntirelyUris);
  }

  private Set<String> deconstructConfiguration(String toSplit)  {
    return Stream.of(toSplit.split(","))
      .filter(Objects::nonNull)
      .map(String::trim)
      .filter(s -> s.length() > 0)
      .collect(Collectors.toSet());
  }

  /**
   * Return true if the body payload should not be logged.
   */
  public boolean excludePayloadForUri(String uriPath) {
    if (excludeUri.contains(uriPath)) {
      if (JerseyLoggerPoint.logger.isTraceEnabled()) {
        // mention we are excluding payload logging
        ConnectContext.set(Constants.REST_CONTEXT, "exclude payload logging for uriPath:" + uriPath);
        JerseyLoggerPoint.logger.trace("no payload");
        ConnectContext.remove(Constants.REST_CONTEXT);
      }
      return true;
    }
    return false;
  }

  /**
   * should we exclude this reference entirely?
   */
  public boolean excludeForUri(String uriPath) {
    return excludeEntirelyUri.contains(uriPath);
  }

  @Override
  public int maxBodySize() {
    return bufferSize;
  }

  public String getExclude() {
    return exclude;
  }

  public String getTracing() {
    return tracing;
  }

  @Override
  public String getVerbosity() {
    return verbosity;
  }
}

