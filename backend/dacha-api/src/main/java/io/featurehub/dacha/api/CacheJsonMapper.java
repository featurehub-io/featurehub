package io.featurehub.dacha.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 *
 */
public class CacheJsonMapper {
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(CacheJsonMapper.class);
  public static ObjectMapper mapper;

  static {
    mapper = new ObjectMapper();
    mapper.registerModule(new JavaTimeModule());
    mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
    mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
    mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
  }

  static public byte[] writeAsZipBytes(Object o) throws IOException {
    byte[] data = mapper.writeValueAsBytes(o);
    final ByteArrayOutputStream baos = new ByteArrayOutputStream(data.length);
    GZIPOutputStream gzip = new GZIPOutputStream(baos);
    gzip.write(data, 0, data.length);
    gzip.flush();
    baos.flush();
    gzip.close();
    baos.close();
    return baos.toByteArray();
  }

  static public <T> T readFromZipBytes(byte[] data, Class<T> clazz) throws IOException {
    log.debug("byte array is {} size", data.length);
    try (ByteArrayInputStream bais = new ByteArrayInputStream(data); GZIPInputStream is = new GZIPInputStream(bais)) {
      return mapper.readValue(is, clazz);
    } catch (Exception ignored) {
      // not a valid gzip stream
    }

    // still a string
    return mapper.readValue(data, clazz);
  }
}
