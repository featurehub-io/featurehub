package io.featurehub.jersey.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.cloudevents.CloudEvent;
import io.cloudevents.core.v1.CloudEventBuilder;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import org.jetbrains.annotations.Nullable;

/** */
public class CacheJsonMapper {
  private static final org.slf4j.Logger log =
      org.slf4j.LoggerFactory.getLogger(CacheJsonMapper.class);
  public static ObjectMapper mapper = JacksonObjectProvider.mapper;

  public static byte[] writeAsZipBytes(Object o) throws IOException {
    byte[] data =
        (o instanceof String)
            ? ((String) o).getBytes(StandardCharsets.UTF_8)
            : mapper.writeValueAsBytes(o);
    ByteArrayOutputStream byteStream = null;
    try (ByteArrayOutputStream baos = new ByteArrayOutputStream(data.length);
        GZIPOutputStream gzip = new GZIPOutputStream(baos)) {
      gzip.write(data, 0, data.length);
      gzip.flush();
      baos.flush();
      byteStream = baos;
    } catch (IOException e) {
      log.error("Unable to write object as zip", e);
      throw e;
    }

    return byteStream.toByteArray();
  }

  public static <T> T readFromZipBytes(byte[] data, Class<T> clazz) throws IOException {
    try (ByteArrayInputStream bais = new ByteArrayInputStream(data);
        GZIPInputStream is = new GZIPInputStream(bais)) {
      if (clazz.equals(String.class)) {
        return (T) new String(is.readAllBytes());
      }

      if (log.isTraceEnabled()) {
        String logSt = new String(is.readAllBytes());
        log.trace("Zipped message is {}", logSt);
        return mapper.readValue(logSt, clazz);
      }

      return mapper.readValue(is, clazz);
    } catch (Exception ignored) {
      // not a valid gzip stream
    }

    // still a string
    return mapper.readValue(data, clazz);
  }

  @Nullable
  public static <T> T fromEventData(CloudEvent event, Class<T> clazz) throws IOException {
    if (event.getData() == null) {
      return null;
    }

    if ("application/json+gzip".equals(event.getDataContentType())) {
      return readFromZipBytes(event.getData().toBytes(), clazz);
    } else if (!"application/json".equals(event.getDataContentType())) {
      throw new IllegalArgumentException("Unknown format, cannot decode");
    }

    return mapper.readValue(event.getData().toBytes(), clazz);
  }

  public static CloudEventBuilder toEventData(
      CloudEventBuilder builder, Object data, boolean compress) throws IOException {
    if (compress) {
      builder.withData("application/json+gzip", writeAsZipBytes(data));
    } else {
      builder.withData("application/json", mapper.writeValueAsBytes(data));
    }

    return builder;
  }
}
