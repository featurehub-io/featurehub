package org.glassfish.jersey.logging;

import cd.connect.context.ConnectContext;
import cd.connect.jersey.common.logging.JerseyFiltering;
import cd.connect.jersey.common.logging.JerseyLoggerPoint;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.WriterInterceptor;
import jakarta.ws.rs.ext.WriterInterceptorContext;
import org.glassfish.jersey.message.MessageUtils;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Base for both client and server jersey tracing.
 */
abstract class BaseFilteringLogger implements WriterInterceptor {

    private final static String OBFUSCATED = "(OBFUSCATED)";

    private final static Pattern PATTERN_AUTHORIZATION_HEADER = Pattern.compile("^([A-Za-z0-9]+)\\s+(.+)$");

    private final static String AUTHORIZATION_HEADER_TYPE_BASIC = "basic";

	@SuppressWarnings("NonConstantLogger")
	final org.slf4j.Logger logger = JerseyLoggerPoint.logger;
	final AtomicLong _id = new AtomicLong(0);
	final LoggingFeature.Verbosity verbosity;
	final int maxEntitySize;

	protected final JerseyFiltering jerseyFiltering;

	/**
	 * Creates a logging filter with custom logger and entity logging turned on, but potentially limiting the size
	 * of entity to be buffered and logged. Verbosity is defined by {@link JerseyFiltering#getVerbosity()}
     * and can be configured via jersey.logging.verbosity environment variable.
	 *
	 */
    BaseFilteringLogger(JerseyFiltering jerseyFiltering) {
        this.jerseyFiltering = jerseyFiltering;
        this.verbosity = resolveVerbosity(jerseyFiltering.getVerbosity());
        this.maxEntitySize = Math.max(0, jerseyFiltering.maxBodySize());
    }

    private LoggingFeature.Verbosity resolveVerbosity(String verbosity) {
        try {
            return LoggingFeature.Verbosity.valueOf(verbosity);
        } catch (Exception e) {
            LoggingFeature.Verbosity defaultVerbosity = LoggingFeature.Verbosity.PAYLOAD_ANY;
            String values = Arrays.toString(LoggingFeature.Verbosity.values());
            logger.warn("Failed to parse '{}' as verbosity, allowed values are: {}. Defaulting to {}",
                    verbosity, values, defaultVerbosity, e);
            return defaultVerbosity;
        }
    }

    /**
	 * Prefix will be printed before requests
	 */
	static final String REQUEST_PREFIX = "> ";
	/**
	 * Prefix will be printed before response
	 */
	static final String RESPONSE_PREFIX = "< ";
	/**
	 * The entity stream property
	 */
	static final String ENTITY_LOGGER_PROPERTY = LoggingFeature.class.getName() + ".entityLogger";
	/**
	 * Logging record id property
	 */
	static final String LOGGING_ID_PROPERTY = LoggingFeature.class.getName() + ".id";
	private static final String NOTIFICATION_PREFIX = "* ";
	private static final MediaType TEXT_MEDIA_TYPE = new MediaType("text", "*");

	private static final Set<MediaType> READABLE_APP_MEDIA_TYPES = new HashSet<MediaType>() {{
		add(TEXT_MEDIA_TYPE);
		add(MediaType.APPLICATION_ATOM_XML_TYPE);
		add(MediaType.APPLICATION_FORM_URLENCODED_TYPE);
		add(MediaType.APPLICATION_JSON_TYPE);
		add(MediaType.APPLICATION_SVG_XML_TYPE);
		add(MediaType.APPLICATION_XHTML_XML_TYPE);
		add(MediaType.APPLICATION_XML_TYPE);
	}};

	private static final Comparator<Map.Entry<String, List<String>>> COMPARATOR =
    (o1, o2) -> o1.getKey().compareToIgnoreCase(o2.getKey());

	/**
	 * Logs a {@link StringBuilder} parameter at required level.
	 *
	 * @param b message to log
	 */
	void log(final StringBuilder b) {
		if (logger != null && logger.isTraceEnabled()) {
			logger.trace("{}", b.toString());
		}


		ConnectContext.remove(Constants.REST_CONTEXT);
		ConnectContext.remove(Constants.REST_STATUS_CODE);
		ConnectContext.remove(Constants.REST_TIMING);
	}

	private StringBuilder prefixId(final StringBuilder b, final long id) {
		b.append(id).append(" ");
		return b;
	}

	void printRequestLine(final StringBuilder b, final String note, final long id, final String method, final URI uri) {
		prefixId(b, id).append(NOTIFICATION_PREFIX)
			.append(note)
			.append(" on thread ").append(Thread.currentThread().getName())
			.append("\n");
		prefixId(b, id).append(REQUEST_PREFIX).append(method).append(" ")
			.append(uri.toASCIIString()).append("\n");
	}

	void printResponseLine(final StringBuilder b, final String note, final long id, final int status) {
		prefixId(b, id).append(NOTIFICATION_PREFIX)
			.append(note)
			.append(" on thread ").append(Thread.currentThread().getName()).append("\n");
		prefixId(b, id).append(RESPONSE_PREFIX)
			.append(status)
			.append("\n");
	}

	void printPrefixedHeaders(final StringBuilder b,
	                          final long id,
	                          final String prefix,
	                          final MultivaluedMap<String, String> headers) {
		for (final Map.Entry<String, List<String>> headerEntry : getSortedHeaders(headers.entrySet())) {
			final List<?> val = headerEntry.getValue();
			final String header = headerEntry.getKey();

			if (val.size() == 1) {
				prefixId(b, id).append(prefix)
                        .append(header)
                        .append(": ")
                        .append(mapHeaderValue(header, val.get(0)))
                        .append("\n");
			} else {
				final StringBuilder sb = new StringBuilder();
				boolean add = false;
				for (final Object s : val) {
					if (add) {
						sb.append(',');
					}
					add = true;
					sb.append(mapHeaderValue(header, s));
				}
				prefixId(b, id)
                        .append(prefix)
                        .append(header)
                        .append(": ")
                        .append(sb)
                        .append("\n");
			}
		}
	}

    /**
     * <p>This allows for the values in the headers to not be logged if they are security-related.</p>
     */

	private Object mapHeaderValue(String header, Object value) {
        if (null != value && HttpHeaders.AUTHORIZATION.equalsIgnoreCase(header)) {
            Matcher matcher = PATTERN_AUTHORIZATION_HEADER.matcher(value.toString());

            if (matcher.matches()) {
                String type = matcher.group(1);
                String typeLowerCase = type.toLowerCase();
                String credentials = matcher.group(2);

                if (typeLowerCase.equals(AUTHORIZATION_HEADER_TYPE_BASIC)) {
                    return type + " " + mapBasicCredentials(credentials);
                }

                return type + " " + generalObfuscateCredentials(credentials);
            }

            return OBFUSCATED;
        }

        return value;
    }

    /**
     * <p>This is used for username + password authentication.  This will unbundle the
     * username + password and will just log the username portion for conveniences'
     * sake.</p>
     */

    private String mapBasicCredentials(String value) {
        byte[] plainTextBytes;

        try {
            plainTextBytes = Base64.getDecoder().decode(value);
        }
        catch (IllegalArgumentException iae) {
            return "(CORRUPT-BASE64?)";
        }

        String plainText = new String(plainTextBytes, StandardCharsets.UTF_8);
        int colonIdx = plainText.indexOf(':');

        if (-1 != colonIdx) {
            String username = plainText.substring(0, colonIdx);
            return username + ":" + OBFUSCATED;
        }

        return "(MALFORMED?)";
    }

    private String generalObfuscateCredentials(String value) {
        value = value == null ? "" : value.trim();

        if (value.length() < 6) {
            return OBFUSCATED;
        }

        if (value.length() < 12) {
            return value.charAt(0) + ".." + OBFUSCATED;
        }

        return value.substring(0, 2) + ".." + OBFUSCATED + ".." + value.charAt(value.length() - 1);
    }

	Set<Map.Entry<String, List<String>>> getSortedHeaders(final Set<Map.Entry<String, List<String>>> headers) {
		final TreeSet<Map.Entry<String, List<String>>> sortedHeaders = new TreeSet<>(COMPARATOR);
		sortedHeaders.addAll(headers);
		return sortedHeaders;
	}

	InputStream logInboundEntity(final StringBuilder b, InputStream stream, final Charset charset) throws IOException {
		if (!stream.markSupported()) {
			stream = new BufferedInputStream(stream);
		}
		stream.mark(maxEntitySize + 1);
		final byte[] entity = new byte[maxEntitySize + 1];
		final int entitySize = stream.read(entity);
		b.append(new String(entity, 0, Math.min(entitySize, maxEntitySize), charset));
		if (entitySize > maxEntitySize) {
			b.append("...more...");
		}
		b.append('\n');
		stream.reset();
		return stream;
	}

	@Override
	public void aroundWriteTo(final WriterInterceptorContext writerInterceptorContext)
		throws IOException, WebApplicationException {
		final LoggingStream stream = (LoggingStream) writerInterceptorContext.getProperty(ENTITY_LOGGER_PROPERTY);
		// headers are final by the time we get here (server side the encoding filter runs after the logging filter),
		// so if the entity is going to be compressed, the logging stream would only see the encoded bytes
		final Object contentEncoding = writerInterceptorContext.getHeaders().getFirst(HttpHeaders.CONTENT_ENCODING);
		final boolean encoded = contentEncoding != null && isEncoded(contentEncoding.toString());
		if (stream != null && encoded) {
			stream.disableCapture();
		}
		writerInterceptorContext.proceed();
		if (logger.isTraceEnabled() && printEntity(verbosity, writerInterceptorContext.getMediaType())) {
			if (stream != null) {
				if (encoded) {
					log(stream.getStringBuilderWithoutEntity(contentEncoding.toString()));
				} else {
					log(stream.getStringBuilder(MessageUtils.getCharset(writerInterceptorContext.getMediaType())));
				}
			}
		}
	}

	/**
	 * Returns {@code true} if the content encoding indicates the entity is compressed (i.e. not plain text).
	 *
	 * @param contentEncoding the value of the Content-Encoding header, may be null
	 * @return {@code true} if the entity should not be logged
	 */
	static boolean isEncoded(String contentEncoding) {
		return contentEncoding != null && !contentEncoding.isBlank() && !"identity".equalsIgnoreCase(contentEncoding.trim());
	}

	void printEncodedEntity(final StringBuilder b, final String contentEncoding) {
		b.append("(entity not logged, Content-Encoding: ").append(contentEncoding).append(")\n");
	}

	/**
	 * Returns {@code true} if specified {@link MediaType} is considered textual.
	 * <p>
	 * See {@link #READABLE_APP_MEDIA_TYPES}.
	 *
	 * @param mediaType the media type of the entity
	 * @return {@code true} if specified {@link MediaType} is considered textual.
	 */
	static boolean isReadable(MediaType mediaType) {
		if (mediaType != null) {
			for (MediaType readableMediaType : READABLE_APP_MEDIA_TYPES) {
				if (readableMediaType.isCompatible(mediaType)) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Returns {@code true} if entity has to be printed.
	 *
	 * @param verbosity the configured verbosity .
	 * @param mediaType the media type of the payload.
	 * @return {@code true} if entity has to be printed.
	 */
	static boolean printEntity(LoggingFeature.Verbosity verbosity, MediaType mediaType) {
		return verbosity == LoggingFeature.Verbosity.PAYLOAD_ANY || (verbosity == LoggingFeature.Verbosity.PAYLOAD_TEXT && isReadable(mediaType));
	}

	/**
	 * Helper class used to log an entity to the output stream up to the specified maximum number of bytes.
	 */
	class LoggingStream extends FilterOutputStream {

		private final StringBuilder b;
		private final ByteArrayOutputStream baos = new ByteArrayOutputStream();
		private boolean capture = true;

		/**
		 * Creates {@code LoggingStream} with the entity and the underlying output stream as parameters.
		 *
		 * @param b     contains the entity to log.
		 * @param inner the underlying output stream.
		 */
		LoggingStream(final StringBuilder b, final OutputStream inner) {
			super(inner);

			this.b = b;
		}

		StringBuilder getStringBuilder(final Charset charset) {
			// write entity to the builder
			final byte[] entity = baos.toByteArray();

			b.append(new String(entity, 0, Math.min(entity.length, maxEntitySize), charset));
			if (entity.length > maxEntitySize) {
				b.append("...more...");
			}
			b.append('\n');

			return b;
		}

		void disableCapture() {
			capture = false;
			baos.reset();
		}

		StringBuilder getStringBuilderWithoutEntity(final String contentEncoding) {
			printEncodedEntity(b, contentEncoding);
			return b;
		}

		@Override
		public void write(final int i) throws IOException {
			if (capture && baos.size() <= maxEntitySize) {
				baos.write(i);
			}
			out.write(i);
		}
	}
}
