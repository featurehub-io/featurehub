package org.glassfish.jersey.logging;

import cd.connect.context.ConnectContext;
import cd.connect.jersey.common.logging.JerseyFiltering;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.ws.rs.ConstrainedTo;
import jakarta.ws.rs.RuntimeType;
import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;
import jakarta.ws.rs.client.ClientResponseContext;
import jakarta.ws.rs.client.ClientResponseFilter;
import jakarta.ws.rs.core.HttpHeaders;
import org.glassfish.jersey.message.MessageUtils;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;

/**
 * Note Jersey's ClientLoggingFilter is final so this is effectively the replacement for that.
 * <p>
 * This takes into account excluding payload logging on special uri's that contain sensitive content.
 * </p>
 */
@ConstrainedTo(RuntimeType.CLIENT)
@Priority(Integer.MAX_VALUE)
@Singleton
@SuppressWarnings("ClassWithMultipleLoggers")
public class FilteringClientLoggingFilter extends BaseFilteringLogger implements ClientRequestFilter, ClientResponseFilter {

  @Inject
	public FilteringClientLoggingFilter(JerseyFiltering jerseyFiltering) {
		super(jerseyFiltering);
	}

	protected void recordOutgoing(ClientRequestContext context, String action) {
		ConnectContext.set(Constants.REST_CONTEXT, action + ": " + context.getMethod() + " - " + context.getUri().toASCIIString());
	}

	@Override
	public void filter(final ClientRequestContext context) throws IOException {
		String uriPath = context.getUri().getPath();
		if (!logger.isTraceEnabled() || jerseyFiltering.excludeForUri(uriPath)) {
			return;
		}
		long id = _id.incrementAndGet();
		context.setProperty(LOGGING_ID_PROPERTY, id);
		context.setProperty(Constants.REST_TIMING, System.currentTimeMillis());

		StringBuilder b = new StringBuilder();

		URI uri = context.getUri();
		recordOutgoing(context, "sending");

		printRequestLine(b, "Sending client request", id, context.getMethod(), uri);
		printPrefixedHeaders(b, id, REQUEST_PREFIX, context.getStringHeaders());


		String contentEncoding = context.getHeaderString(HttpHeaders.CONTENT_ENCODING);
		if (context.hasEntity() && isEncoded(contentEncoding)) {
			printEncodedEntity(b, contentEncoding);
			log(b);
		} else if (context.hasEntity() && printEntity(verbosity, context.getMediaType()) && !jerseyFiltering.excludePayloadForUri(uriPath)) {
			OutputStream stream = new LoggingStream(b, context.getEntityStream());
			context.setEntityStream(stream);
			context.setProperty(ENTITY_LOGGER_PROPERTY, stream);
			// not calling log(b) here - it will be called by the interceptor
		} else {
			log(b);
		}
	}

	@Override
	public void filter(final ClientRequestContext requestContext, final ClientResponseContext responseContext) throws IOException {
		String uriPath = requestContext.getUri().getPath();
		if (!logger.isTraceEnabled() || jerseyFiltering.excludeForUri(uriPath)) {
			return;
		}

		recordOutgoing(requestContext, "response");

		Object requestId = requestContext.getProperty(LOGGING_ID_PROPERTY);
		long id = requestId != null ? (Long) requestId : _id.incrementAndGet();

		StringBuilder b = new StringBuilder();
		printResponseLine(b, "Client response received", id, responseContext.getStatus());
		printPrefixedHeaders(b, id, RESPONSE_PREFIX, responseContext.getHeaders());

		String contentEncoding = responseContext.getHeaderString(HttpHeaders.CONTENT_ENCODING);
		if (responseContext.hasEntity() && isEncoded(contentEncoding)) {
			printEncodedEntity(b, contentEncoding);
		} else if (responseContext.hasEntity() && printEntity(verbosity, responseContext.getMediaType()) && !jerseyFiltering.excludePayloadForUri(uriPath)) {
			responseContext.setEntityStream(logInboundEntity(b, responseContext.getEntityStream(), MessageUtils.getCharset(responseContext.getMediaType())));
		}

		Object originalStart = requestContext.getProperty(Constants.REST_TIMING);
		if (originalStart != null) {
			if (originalStart instanceof String) {
				requestContext.removeProperty(Constants.REST_TIMING);
			} else {
				Long start = (Long) originalStart;
				long end = System.currentTimeMillis();
				ConnectContext.set(Constants.REST_TIMING, (end - start) + "");
			}
		}

		log(b);
	}
}
