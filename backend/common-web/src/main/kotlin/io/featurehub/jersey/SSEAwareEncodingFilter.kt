package io.featurehub.jersey

import jakarta.annotation.Priority
import jakarta.inject.Inject
import jakarta.ws.rs.ConstrainedTo
import jakarta.ws.rs.Priorities
import jakarta.ws.rs.RuntimeType
import jakarta.ws.rs.container.ContainerRequestContext
import jakarta.ws.rs.container.ContainerResponseContext
import jakarta.ws.rs.container.ContainerResponseFilter
import jakarta.ws.rs.core.EntityTag
import jakarta.ws.rs.core.HttpHeaders
import jakarta.ws.rs.core.MediaType
import org.glassfish.jersey.internal.inject.InjectionManager
import org.glassfish.jersey.media.sse.EventOutput
import org.glassfish.jersey.server.filter.EncodingFilter

/**
 * Wraps Jersey's EncodingFilter so that SSE streams are never content-encoded (gzip buffers the stream,
 * which stops events reaching clients in real time). When a response does get encoded, any strong ETag
 * is downgraded to a weak one as the encoded bytes differ from the identity representation.
 */
@ConstrainedTo(RuntimeType.SERVER)
@Priority(Priorities.HEADER_DECORATOR)
class SSEAwareEncodingFilter @Inject constructor(injectionManager: InjectionManager) : ContainerResponseFilter {
  private val delegate: EncodingFilter = injectionManager.createAndInitialize(EncodingFilter::class.java)

  override fun filter(request: ContainerRequestContext, response: ContainerResponseContext) {
    if (!response.hasEntity() || isSse(response)) {
      return
    }

    delegate.filter(request, response)

    val encoding = response.headers.getFirst(HttpHeaders.CONTENT_ENCODING)?.toString()
    if (encoding != null && encoding != "identity") {
      weakenEtag(response)
    }
  }

  private fun isSse(response: ContainerResponseContext): Boolean {
    return response.entityType == EventOutput::class.java ||
      EventOutput::class.java.isInstance(response.entity) ||
      response.mediaType?.isCompatible(MediaType.SERVER_SENT_EVENTS_TYPE) == true
  }

  private fun weakenEtag(response: ContainerResponseContext) {
    when (val etag = response.headers.getFirst(HttpHeaders.ETAG)) {
      is EntityTag -> if (!etag.isWeak) response.headers.putSingle(HttpHeaders.ETAG, EntityTag(etag.value, true))
      is String -> if (etag.startsWith("\"")) response.headers.putSingle(HttpHeaders.ETAG, "W/$etag")
    }
  }
}
