package io.featurehub.edge.features

import io.featurehub.edge.KeyParts

class EtagStructureHolder(val environmentTags: Map<KeyParts, String>, val contextTag: String, val validEtag: Boolean)

class ETagSplitter {
  companion object {
    /**
     * The first wire-protocol version at which an SDK declares (via the `sdk_version` parameter) that it understands
     * SSE event ids are etags and that it will send the last one back as `Last-Event-ID` on reconnect.
     */
    const val SDK_VERSION_LAST_EVENT_ID = 2

    /**
     * Should we honour a Last-Event-ID presented on an SSE connection? Only if the SDK has told us it speaks a
     * wire-protocol version that understands it.
     */
    fun honourLastEventId(sdkVersion: Int?): Boolean =
      sdkVersion != null && sdkVersion >= SDK_VERSION_LAST_EVENT_ID

    /**
     * Returns the Last-Event-ID to use for this SSE connection, or null if it should be ignored.
     */
    fun sseLastEventId(lastEventId: String?, sdkVersion: Int?): String? =
      if (honourLastEventId(sdkVersion)) lastEventId else null

    /**
     * Parses the sdk_version parameter leniently - an SDK sending something we can't understand is treated as having
     * not sent one at all rather than failing the request.
     */
    fun parseSdkVersion(sdkVersion: String?): Int? = sdkVersion?.trim()?.toIntOrNull()

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
