package io.featurehub.edge.features

import io.featurehub.edge.KeyParts

class EtagStructureHolder(val environmentTags: Map<KeyParts, String>, val contextTag: String, val validEtag: Boolean)

class ETagSplitter {
  companion object {
    fun splitTag(etag: String?, keys: List<KeyParts>, clientContextTag: String): EtagStructureHolder {
      if (etag == null) return EtagStructureHolder(mapOf(), clientContextTag, false)

      val environmentTagVsContextTag = etag.trim().split("//").toTypedArray()

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
