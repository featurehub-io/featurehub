package cd.connect.jersey.common.logging;

/**
 * This class must be usable as soon as it is injected. Typically the payload exclusions
 * can change at runtime, but the filter definitions cannot.
 *
 *
 */
public interface JerseyFiltering {
	boolean excludePayloadForUri(String uriPath);
	boolean excludeForUri(String uriPath);
	int maxBodySize();
	String getExclude();
	String getTracing();
	String getVerbosity();
}
