package io.featurehub.web.security.oauth.providers

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import java.util.*

object Jwt {
  private val log = LoggerFactory.getLogger(Jwt::class.java)
  private val mapper = ObjectMapper()
  fun decodeJwt(jwt: String?): Map<String, Any?>? {
    val parts = jwt!!.split("\\.".toRegex()).toTypedArray()
    if (parts.size != 3) {
      return null
    }
    val body = String(Base64.getUrlDecoder().decode(parts[1]))
    return try {
      mapper.readValue(body, object : TypeReference<Map<String, Any?>?>() {})
    } catch (e: JsonProcessingException) {
      log.error("Could not parse result of OAuth2 JWT {}", jwt, e)
      null
    }
  }
}
