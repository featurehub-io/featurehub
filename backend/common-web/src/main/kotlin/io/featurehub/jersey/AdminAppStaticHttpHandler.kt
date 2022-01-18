package io.featurehub.jersey

import io.featurehub.utils.FallbackPropertyConfig
import org.glassfish.grizzly.WriteHandler
import org.glassfish.grizzly.http.io.NIOOutputStream
import org.glassfish.grizzly.http.server.Request
import org.glassfish.grizzly.http.server.Response
import org.glassfish.grizzly.http.server.StaticHttpHandler
import org.glassfish.grizzly.http.util.Header
import org.glassfish.grizzly.http.util.HttpStatus
import org.glassfish.grizzly.http.util.MimeType
import org.glassfish.grizzly.memory.Buffers
import org.glassfish.grizzly.memory.MemoryManager
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.io.IOException

class AdminAppStaticHttpHandler constructor(private val offsetBaseUrl: String) :
  StaticHttpHandler(setOf(FallbackPropertyConfig.getConfig("web.asset.location", "/var/www/html"))) {
  private val log: Logger = LoggerFactory.getLogger(AdminAppStaticHttpHandler::class.java)
  private val indexResource: File
  private val html: String

  init {
    MimeType.add("wasm", "application/wasm")

    indexResource = preloadIndexHtml()
    html = loadIndexHtml(indexResource)

    log.trace("New html file is {}", html)
  }

  private fun loadIndexHtml(resource: File): String {
    val url = if (offsetBaseUrl.endsWith("/")) offsetBaseUrl.substring(0, offsetBaseUrl.length-1) else offsetBaseUrl
    return resource.readText(Charsets.UTF_8).replace("<base href=\"/\">",
      "<base href=\"${url}/\">")
  }

  private fun preloadIndexHtml(): File {
    val fileFolders = docRoots.array as Array<File>
    val uri = "index.html"
    var resource: File? = null
    var found = false

    for (webDir in fileFolders) {
      resource = File(webDir, uri)
      found = resource.exists()
    }

    if (!found) {
      throw RuntimeException("Unable to find index.html")
    }


    return resource!!
  }

  internal class NonBlockingBufferHandler(
    response: Response,
    outputStream: NIOOutputStream,
    private val text: String
  ) : WriteHandler {
    // keep the remaining size
    private val response: Response
    private val outputStream: NIOOutputStream
    private val mm: MemoryManager<*>

    init {
      this.response = response
      this.outputStream = outputStream
      mm = response.request.context.memoryManager
    }

    @Throws(Exception::class)
    override fun onWritePossible() {
      sendChunk()
    }

    override fun onError(t: Throwable) {
      response.setStatus(500, t.message)
      complete(true)
    }

    /**
     * Send the entire index html file.
     */
    @Throws(IOException::class)
    private fun sendChunk() {
      val buffer = Buffers.wrap(mm, text, Charsets.UTF_8)
      // mark it available for disposal after content is written
      buffer.allowBufferDispose(true)

      // write the Buffer
      outputStream.write(buffer)

      complete(false)
    }

    /**
     * Complete the download
     */
    private fun complete(isError: Boolean) {
      try {
        outputStream.close()
      } catch (e: IOException) {
        if (!isError) {
          response.setStatus(500, e.message)
        }
      }

      if (response.isSuspended) {
        response.resume()
      } else {
        response.finish()
      }
    }
  }

  @Throws(IOException::class)
  fun sendFile(response: Response) {
    response.setStatus(HttpStatus.OK_200)

    // In case this sendFile(...) is called directly by user - pickup the content-type
    pickupContentType(response, "index.html")
//    val length = html.length
//    response.contentLengthLong = length.toLong()
    response.addDateHeader(Header.Date, System.currentTimeMillis())

    response.suspend()

    val outputStream = response.nioOutputStream

    outputStream.notifyCanWrite(NonBlockingBufferHandler(response, outputStream, html))
  }

  public override fun handle(uri: String?, request: Request?, response: Response?): Boolean {
    if (response != null && (uri == "/index.html" || uri == "index.html" || uri == null || uri == "/")) {
      sendFile(response!!)
      return true
    }

    return super.handle(uri, request, response)
  }
}
