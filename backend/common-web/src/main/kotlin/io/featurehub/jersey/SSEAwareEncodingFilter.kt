package io.featurehub.jersey

import jakarta.annotation.Priority
import jakarta.inject.Inject
import jakarta.ws.rs.NotAcceptableException
import jakarta.ws.rs.container.ContainerRequestContext
import jakarta.ws.rs.container.ContainerResponseContext
import jakarta.ws.rs.container.ContainerResponseFilter
import org.glassfish.jersey.internal.inject.InjectionManager
import org.glassfish.jersey.media.sse.EventOutput
import org.glassfish.jersey.message.internal.HttpHeaderReader
import org.glassfish.jersey.server.filter.EncodingFilter
import org.glassfish.jersey.spi.ContentEncoder
import java.io.IOException
import java.text.ParseException
import java.util.*
import java.util.logging.Level
import java.util.logging.Logger

@Priority(3000)
class SSEAwareEncodingFilter @Inject constructor(private val injectionManager: InjectionManager) : ContainerResponseFilter {
  val supportedEncodings = mutableSetOf<String>()

  init {
    for (encoder in this.injectionManager.getAllInstances<ContentEncoder>(ContentEncoder::class.java)) {
      supportedEncodings.addAll(encoder.supportedEncodings)
    }

    supportedEncodings.add("identity")
  }

  @Throws(IOException::class)
  override fun filter(request: ContainerRequestContext, response: ContainerResponseContext) {
    if (response.hasEntity()) {
      val varyHeader = response.stringHeaders["Vary"]
      if (varyHeader == null || !varyHeader.contains("Accept-Encoding")) {
        response.headers.add("Vary", "Accept-Encoding")
      }


      // ensure the content-encoding isn't already set
      // also ensure that this isn't an SSE response (this is our main change)
      if (response.entityType != EventOutput::class.java && response.headers.getFirst("Content-Encoding") == null && response.headers.getFirst("Content-Type") != "text/event-stream") {
        val acceptEncoding = request.headers["Accept-Encoding"]
        if (!acceptEncoding.isNullOrEmpty()) {
          val encodings = mutableListOf<ContentEncoding>()

          for (input in acceptEncoding) {
            if (!input.isEmpty()) {
              val tokens: Array<String?> = input.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()

              for (token in tokens) {
                try {
                  token?.let {
                    val encoding = ContentEncoding.fromString(it)
                    encodings.add(encoding)
                  }
                } catch (e: ParseException) {
                  Logger.getLogger(EncodingFilter::class.java.getName()).log(Level.WARNING, e.localizedMessage, e)
                }
              }
            }
          }

          encodings.sort<ContentEncoding>()
          encodings.add(ContentEncoding("identity", -1))

          val acceptedEncodings = TreeSet(this.supportedEncodings)

          var anyRemaining = false
          var contentEncoding: String? = null

          for (encoding in encodings) {
            if (encoding.q == 0) {
              if ("*" == encoding.name) {
                break
              }

              acceptedEncodings.remove(encoding.name)
            } else if ("*" == encoding.name) {
              anyRemaining = true
            } else if (acceptedEncodings.contains(encoding.name)) {
              contentEncoding = encoding.name
              break
            }
          }

          if (contentEncoding == null) {
            if (!anyRemaining || acceptedEncodings.isEmpty()) {
              throw NotAcceptableException()
            }

            contentEncoding = acceptedEncodings.first()
          }

          if ("identity" != contentEncoding) {
            response.headers.putSingle("Content-Encoding", contentEncoding)
          }
        }
      }
    }
  }

  private class ContentEncoding(val name: String, val q: Int) : Comparable<ContentEncoding> {
    override fun hashCode(): Int {
      return 41 * this.name.hashCode() + this.q
    }

    override fun equals(other: Any?): Boolean {
      return other === this || other != null && other is ContentEncoding && this.name == other.name && this.q == other.q
    }

    override fun compareTo(other: ContentEncoding): Int {
      return other.q.compareTo(this.q)
    }

    companion object {
      @Throws(ParseException::class)
      fun fromString(input: String): ContentEncoding {
        val reader = HttpHeaderReader.newInstance(input)
        reader.hasNext()
        return ContentEncoding(reader.nextToken().toString(), HttpHeaderReader.readQualityFactorParameter(reader))
      }
    }
  }
}
