package io.featurehub.jersey

import spock.lang.Specification
import spock.lang.TempDir
import spock.lang.Unroll

class PrecompressedAssetsSpec extends Specification {
  @TempDir
  File webDir

  File asset(String name) {
    def f = new File(webDir, name)
    f.text = "lingling kwong and orm kornaphat"
    return f
  }

  @Unroll
  def "accept-encoding '#header' for #encoding is #accepted"() {
    expect:
      PrecompressedAssets.INSTANCE.acceptsEncoding(header, encoding) == accepted

    where:
      header                         | encoding | accepted
      null                           | "gzip"   | false
      ""                             | "gzip"   | false
      "gzip"                         | "gzip"   | true
      "GZIP"                         | "gzip"   | true
      "x-gzip"                       | "gzip"   | true
      "gzip, deflate, br"            | "br"     | true
      "gzip, deflate"                | "br"     | false
      "gzip;q=0"                     | "gzip"   | false
      "gzip; q=0.0, br"              | "gzip"   | false
      "gzip;q=0.5"                   | "gzip"   | true
      "*"                            | "br"     | true
      "*;q=0"                        | "br"     | false
      "br;q=0, *"                    | "br"     | false
      "*, gzip;q=0"                  | "gzip"   | false
      "identity"                     | "gzip"   | false
  }

  def "brotli is preferred over gzip when both exist and are accepted"() {
    given:
      def js = asset("main-ormkorn.js")
      asset("main-ormkorn.js.gz")
      asset("main-ormkorn.js.br")
    when:
      def v = PrecompressedAssets.INSTANCE.select(js, "gzip, deflate, br")
    then:
      v.encoding == "br"
      v.file.name == "main-ormkorn.js.br"
      PrecompressedAssets.INSTANCE.hasVariants(js)
  }

  def "falls back to gzip when brotli is not accepted"() {
    given:
      def js = asset("main-ormkorn.js")
      asset("main-ormkorn.js.gz")
      asset("main-ormkorn.js.br")
    when:
      def v = PrecompressedAssets.INSTANCE.select(js, "gzip")
    then:
      v.encoding == "gzip"
      v.file.name == "main-ormkorn.js.gz"
  }

  def "falls back to gzip when brotli file does not exist"() {
    given:
      def js = asset("canvaskit.wasm")
      asset("canvaskit.wasm.gz")
    expect:
      PrecompressedAssets.INSTANCE.select(js, "br, gzip").encoding == "gzip"
  }

  def "no variant when the client accepts no supported encoding or none exist"() {
    given:
      def js = asset("main-ormkorn.js")
      asset("main-ormkorn.js.gz")
      def png = asset("kwong-pastry.png")
    expect:
      PrecompressedAssets.INSTANCE.select(js, "identity") == null
      PrecompressedAssets.INSTANCE.select(js, null) == null
      PrecompressedAssets.INSTANCE.select(png, "gzip, br") == null
      !PrecompressedAssets.INSTANCE.hasVariants(png)
  }

  def "a request for the compressed file itself is not double handled"() {
    given:
      asset("main-ormkorn.js")
      def gz = asset("main-ormkorn.js.gz")
      asset("main-ormkorn.js.gz.gz")
    expect:
      PrecompressedAssets.INSTANCE.select(gz, "gzip") == null
      !PrecompressedAssets.INSTANCE.hasVariants(gz)
  }
}
