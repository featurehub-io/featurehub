package io.featurehub.edge.client

import jakarta.ws.rs.core.MediaType
import org.glassfish.jersey.media.sse.EventOutput
import org.glassfish.jersey.media.sse.OutboundEvent
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.IOException
import kotlin.jvm.Throws

interface EventOutputHolder {
  @Throws(IOException::class)
  fun write(name: String, mediaType: MediaType, etags: String?, data: String)
  fun isClosed(): Boolean
  fun close()
}

class InternalEventOutput(private val output: EventOutput) : EventOutputHolder {
  private val log: Logger = LoggerFactory.getLogger(InternalEventOutput::class.java)

  override fun write(name: String, mediaType: MediaType, etags: String?, data: String) {
    val eventBuilder = OutboundEvent.Builder()
    log.trace("data is  etag `{}`: name: `{}` data `{}`", etags, name, data)
    eventBuilder.name(name)
    eventBuilder.mediaType(mediaType)
    if (etags != null) {
      eventBuilder.id(etags)
    }
    eventBuilder.data(data)
    val event = eventBuilder.build()
    output.write(event)
  }

  override fun isClosed(): Boolean {
    return output.isClosed
  }

  override fun close() {
    output.close()
  }

}
