package io.featurehub.edge.features

import io.featurehub.edge.KeyParts
import io.featurehub.utils.FallbackPropertyConfig

class EtagStructureHolder(val environmentTags: Map<KeyParts, String>, val contextTag: String, val validEtag: Boolean)

class ETagSplitter {
  companion object {
    val etagsEnabledForSse = FallbackPropertyConfig.getConfig("edge.etags-for-sse-enabled", "false") == "true"

    fun makeEtagHolderForSSE(etag: String?, keys: List<KeyParts>, etagFromClientContext: String): EtagStructureHolder {
      // if we aren't presented with an etag or the number of keys we are looking at is > 1 (which seems impossible now because SSE doesn't support it)
      // then we assume no etag
      if (!etagsEnabledForSse) {
        return EtagStructureHolder(emptyMap(), etagFromClientContext, false)
      }

      return parseSseEtag(etag, keys, etagFromClientContext)
    }

    fun parseSseEtag(etag: String?, keys: List<KeyParts>, etagFromClientContext: String): EtagStructureHolder {
      if (etag == null || keys.size != 1) {
        return EtagStructureHolder(emptyMap(), etagFromClientContext, false)
      }

      // the SSE event id is the same structure as the REST etag (see makeEtags) - `<env-etag>[//<context-tag>]` - but
      // without the quotes. We must honour the context part - if the client's context has changed since it was given
      // this id (or it now has one and didn't before, or vice versa), the features it holds were evaluated against a
      // different context and it must get a full set again. splitTag does exactly that check, so normalise and reuse it.
      val bareTag = etag.trim().removePrefix("W/").removeSurrounding("\"")
      if (bareTag.isEmpty()) {
        return EtagStructureHolder(emptyMap(), etagFromClientContext, false)
      }

      return splitTag("\"$bareTag\"", keys, etagFromClientContext)
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
