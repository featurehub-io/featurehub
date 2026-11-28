package io.featurehub.jersey

import java.io.File

/**
 * The admin frontend build (admin-frontend/build/build.sh) writes .br and .gz copies next to the
 * compressible static assets (main-<sha>.js, canvaskit wasm, etc). This picks the best one the
 * client will accept so it can be sent as-is with a Content-Encoding header, avoiding compressing
 * the same immutable file on every request.
 */
object PrecompressedAssets {
  data class Variant(val file: File, val encoding: String)

  // in order of preference
  private val encodings = listOf("br" to ".br", "gzip" to ".gz")

  /**
   * true if any precompressed copy of this resource exists, in which case the response varies on Accept-Encoding
   * regardless of which representation we send.
   */
  fun hasVariants(resource: File): Boolean {
    return !isPrecompressedFile(resource) && encodings.any { (_, suffix) -> File(resource.path + suffix).isFile }
  }

  fun select(resource: File, acceptEncoding: String?): Variant? {
    if (acceptEncoding.isNullOrBlank() || isPrecompressedFile(resource)) {
      return null
    }

    for ((encoding, suffix) in encodings) {
      if (acceptsEncoding(acceptEncoding, encoding)) {
        val file = File(resource.path + suffix)
        if (file.isFile) {
          return Variant(file, encoding)
        }
      }
    }

    return null
  }

  private fun isPrecompressedFile(resource: File): Boolean {
    return encodings.any { (_, suffix) -> resource.name.endsWith(suffix) }
  }

  /**
   * RFC 9110 Accept-Encoding: an explicitly listed coding wins, otherwise "*" applies, otherwise not acceptable.
   * A q value of 0 means "not acceptable". x-gzip is treated as an alias of gzip.
   */
  fun acceptsEncoding(acceptEncoding: String?, encoding: String): Boolean {
    if (acceptEncoding.isNullOrBlank()) {
      return false
    }

    var wildcard: Boolean? = null

    for (part in acceptEncoding.split(',')) {
      val pieces = part.split(';')
      var name = pieces[0].trim().lowercase()
      if (name == "x-gzip") {
        name = "gzip"
      }

      if (name.isEmpty()) {
        continue
      }

      val q = pieces.drop(1)
        .map { it.trim() }
        .firstOrNull { it.startsWith("q=", ignoreCase = true) }
        ?.substring(2)?.trim()?.toDoubleOrNull() ?: 1.0

      if (name == encoding) {
        return q > 0
      }

      if (name == "*") {
        wildcard = q > 0
      }
    }

    return wildcard ?: false
  }
}
