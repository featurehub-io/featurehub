package io.featurehub.jersey

import org.glassfish.grizzly.http.server.HttpServer
import org.glassfish.grizzly.http.server.NetworkListener
import spock.lang.Specification
import spock.lang.TempDir

import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

class AdminAppStaticHttpHandlerSpec extends Specification {
  @TempDir
  File webDir

  HttpServer server
  int port
  String js = "console.log('lingling kwong, orm kornaphat, always wonder');\n" * 50

  def setup() {
    new File(webDir, "index.html").text = '<html><head><base href="/"></head></html>'
    new File(webDir, "main-ormkorn.js").text = js
    new File(webDir, "main-ormkorn.js.gz").withOutputStream { os ->
      new GZIPOutputStream(os).withCloseable { it.write(js.getBytes("UTF-8")) }
    }
    new File(webDir, "kwong-pastry.css").text = "body { color: pink; }"
    // the WebAssembly build's JavaScript support module and the module itself
    gzipped("main-chago.mjs", js.getBytes("UTF-8"))
    gzipped("main-chago.wasm", ([0, 97, 115, 109, 1, 0, 0, 0] * 200) as byte[])

    System.setProperty("web.asset.location", webDir.absolutePath)

    server = new HttpServer()
    def listener = new NetworkListener("test", "127.0.0.1", 0)
    server.addListener(listener)
    server.serverConfiguration.addHttpHandler(new DelegatingHandler(new AdminAppStaticHttpHandler("/")), "/")
    server.start()
    port = listener.port
  }

  void gzipped(String name, byte[] content) {
    new File(webDir, name).bytes = content
    new File(webDir, "${name}.gz").withOutputStream { os ->
      new GZIPOutputStream(os).withCloseable { it.write(content) }
    }
  }

  def cleanup() {
    server?.shutdownNow()
    System.clearProperty("web.asset.location")
  }

  HttpURLConnection get(String path, String acceptEncoding) {
    def conn = (HttpURLConnection) new URL("http://127.0.0.1:${port}${path}").openConnection()
    if (acceptEncoding != null) {
      conn.setRequestProperty("Accept-Encoding", acceptEncoding)
    }
    return conn
  }

  def "precompressed gzip asset is served with content-encoding and the original content type"() {
    when:
      def conn = get("/main-ormkorn.js", "gzip, deflate, br")
      def body = new GZIPInputStream(conn.inputStream).text
    then:
      conn.responseCode == 200
      conn.getHeaderField("Content-Encoding") == "gzip"
      conn.getHeaderField("Vary") == "Accept-Encoding"
      conn.contentType.startsWith("application/javascript") || conn.contentType.startsWith("text/javascript")
      conn.getHeaderField("Content-Length") as long == new File(webDir, "main-ormkorn.js.gz").length()
      body == js
  }

  def "identity is served when the client does not accept gzip"() {
    when:
      def conn = get("/main-ormkorn.js", "identity")
    then:
      conn.responseCode == 200
      conn.getHeaderField("Content-Encoding") == null
      conn.getHeaderField("Vary") == "Accept-Encoding"
      conn.inputStream.text == js
  }

  def "assets with no precompressed copy are served unchanged"() {
    when:
      def conn = get("/kwong-pastry.css", "gzip")
    then:
      conn.responseCode == 200
      conn.getHeaderField("Content-Encoding") == null
      conn.getHeaderField("Vary") == null
      conn.inputStream.text == "body { color: pink; }"
  }

  def "precompressed WebAssembly files keep the content types browsers require"() {
    when:
      def conn = get("/${name}", "gzip")
      def body = new GZIPInputStream(conn.inputStream).bytes
    then:
      conn.responseCode == 200
      conn.getHeaderField("Content-Encoding") == "gzip"
      // a module served as anything but JavaScript is refused, and instantiateStreaming needs application/wasm
      conn.contentType.startsWith(contentType)
      body == new File(webDir, name).bytes
    where:
      name              | contentType
      "main-chago.mjs"  | "text/javascript"
      "main-chago.wasm" | "application/wasm"
  }
}
