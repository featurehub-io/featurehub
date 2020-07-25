package io.featurehub.mr.resources.oauth2.providers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Base64;
import java.util.Map;

public class Jwt {
  private static final Logger log = LoggerFactory.getLogger(Jwt.class);
  private static final ObjectMapper mapper = new ObjectMapper();

  public static Map<String, String> decodeJwt(String jwt) {
    String[] parts = jwt.split("\\.");
    if (parts.length != 3) {
      return null;
    }

    String body = new String(Base64.getUrlDecoder().decode(parts[1]));

    try {
      return mapper.readValue(body, new TypeReference<Map<String, String>>() {});
    } catch (JsonProcessingException e) {
      log.error("Could not parse result of OAuth2 JWT {}", jwt);
      return null;
    }
  }
}
