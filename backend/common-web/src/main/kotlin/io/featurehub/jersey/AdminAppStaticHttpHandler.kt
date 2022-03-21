package io.featurehub.jersey

import cd.connect.app.config.ConfigKey
import cd.connect.app.config.DeclaredConfigResolver
import io.featurehub.utils.FallbackPropertyConfig
import org.glassfish.grizzly.WriteHandler
import org.glassfish.grizzly.http.Method
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
  @ConfigKey("cache-control.web.index")
  var indexCacheControlHeader: String? = "no-store, max-age=0"
  @ConfigKey("cache-control.web.other")
  var restCacheControlHeader: String? = "max-age=864000"

  init {
    MimeType.add("wasm", "application/wasm")

    DeclaredConfigResolver.resolve(this)

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

  // ------------------------------------------------------- Protected Methods
  @Throws(java.lang.Exception::class)
  fun internal_handle(uri: String, request: Request, response: Response): Boolean {
    var found = false
    val fileFolders = docRoots.array ?: return false
    var resource: File? = null
    for (i in fileFolders.indices) {
      val webDir = fileFolders[i]
      // local file
      resource = File(webDir, uri)
      val exists = resource.exists()
      val isDirectory = resource.isDirectory
      if (exists && isDirectory) {
        if (!isDirectorySlashOff && !uri.endsWith("/")) { // redirect to the same url, but with trailing slash
          response.setStatus(HttpStatus.MOVED_PERMANENTLY_301)
          response.setHeader(Header.Location, response.encodeRedirectURL("$uri/"))
          return true
        }
        val f = File(resource, "/index.html")
        if (f.exists()) {
          resource = f
          found = true
          break
        }
      }
      if (isDirectory || !exists) {
        found = false
      } else {
        found = true
        break
      }
    }
    if (!found) {
      if (log.isTraceEnabled) {
        log.trace("File not found {0}", resource)
      }
      return false
    }
    assert(resource != null)

    // If it's not HTTP GET - return method is not supported status
    if (Method.GET != request.method) {
      if (log.isTraceEnabled) {
        log.trace("File found {0}, but HTTP method {1} is not allowed", resource, request.method)
      }
      response.setStatus(HttpStatus.METHOD_NOT_ALLOWED_405)
      response.setHeader(Header.Allow, "GET")
      return true
    }

    response.setHeader("Cache-Control", restCacheControlHeader)
    pickupContentType(response, resource!!.path)
    addToFileCache(request, response, resource)
    sendFile(response, resource)
    return true
  }


  public override fun handle(uri: String?, request: Request, response: Response?): Boolean {
    if (response != null && (uri == "/index.html" || uri == "index.html" || uri == null || uri == "/")) {
      response.setHeader("Cache-Control", indexCacheControlHeader)
      sendFile(response!!)
      return true
    }

    if (uri == null) {
      return false;
    }

    return internal_handle(uri, request, response!!)
  }
}
