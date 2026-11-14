package io.featurehub.edge.features

import io.featurehub.edge.KeyParts
import io.featurehub.utils.FallbackPropertyConfig

class EtagStructureHolder(val environmentTags: Map<KeyParts, String>, val contextTag: String, val validEtag: Boolean)

class ETagSplitter {
  companion object {
    val etagsEnabledForSse = FallbackPropertyConfig.getConfig("edge.etags-for-sse-enabled", "true") == "true"

    fun makeEtagHolderForSSE(etag: String?, keys: List<KeyParts>, etagFromClientContext: String): EtagStructureHolder {
      // if we aren't presented with an etag or the number of keys we are looking at is > 1 (which seems impossible now because SSE doesn't support it)
      // then we assume no etag
      if (!etagsEnabledForSse || etag == null || keys.size != 1) {
        return EtagStructureHolder(emptyMap(), etagFromClientContext, false)
      }

      // otherwise we assume a holder with the API Key => the etag
      return EtagStructureHolder(mapOf(Pair(keys[0], etag)), etagFromClientContext, true)
    }

    fun splitTag(incomingEtag: String?, keys: List<KeyParts>, clientContextTag: String): EtagStructureHolder {
      // compressed responses are given weak etags (see SSEAwareEncodingFilter), the tag itself is the same
      val etag = incomingEtag?.trim()?.removePrefix("W/")
      if (etag == null || !etag.startsWith("\"") || !etag.endsWith("\"")) return EtagStructureHolder(mapOf(), clientContextTag, false)

      val environmentTagVsContextTag = etag.trim().substring(1, etag.length -1).split("//").toTypedArray()

      var contextTags: String

      if (environmentTagVsContextTag.size == 2) {
        contextTags = environmentTagVsContextTag[1].trim()

        if (!clientContextTag.equals(contextTags)) { // different so we will need to transform the features
          return EtagStructureHolder(mapOf(), clientContextTag, false)
        }
      } else if (clientContextTag != "0") {
        return EtagStructureHolder(mapOf(), clientContextTag, false)
      }

      val envTags = mutableMapOf<KeyParts, String>()
      if (environmentTagVsContextTag.isNotEmpty()) {
        val tags = environmentTagVsContextTag[0].trim().split(";").filter { it.isNotEmpty() }.toTypedArray()

        // if the tags != key sizes, its not valid, they have added or removed a key
        if (tags.size != keys.size) {
          return EtagStructureHolder(mapOf(), clientContextTag, false)
        }

        for(count in tags.indices) {
          envTags[keys[count]] = tags[count]
        }
      }

      return EtagStructureHolder(envTags, clientContextTag, true)
    }

    /**
     * makes the new etags with
     */
    fun makeEtags(etags: EtagStructureHolder, responseEtags: List<String>) : String {
      return responseEtags.joinToString(";") + (if (etags.contextTag == "0") "" else  "//" + etags.contextTag)
    }
  }
}
