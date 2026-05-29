package cd.connect.logging;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.layout.AbstractLayout;

@Plugin(
  name = "ConnectJsonLayout",
  category = "Core",
  elementType = "layout",
  printObject = true
)
public class ConnectJsonLayout extends AbstractLayout<LogEvent> {
  protected List<JsonLogEnhancer> loggingProcessors = EnhancerServiceLoader.findJsonLogEnhancers();
  protected boolean prettyPrint;
  protected String prettyPrintSuffix;

  private static final ObjectMapper objectMapper = new ObjectMapper();
  private static final DateTimeFormatter sdf = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

  public ConnectJsonLayout() {
    super(null, null, null);
    prettyPrint = System.getProperty("connect.layout.pretty") != null;
    prettyPrintSuffix = System.getProperty("connect.layout.pretty", "");
  }

  @Override
  public byte[] toByteArray(LogEvent logEvent) {
    Map<String, Object> jsonContext = new HashMap<>();
    List<String> alreadyEncodedJsonObjects = new ArrayList<>();
    Map<String, String> processContext = new HashMap<>();

    processContext.putAll(logEvent.getContextData().toMap());

    try {
      jsonContext.put("@timestamp", sdf.format(Instant.ofEpochMilli(logEvent.getTimeMillis())));

      jsonContext.put("message", logEvent.getMessage().getFormattedMessage());
      jsonContext.put("priority", logEvent.getLevel().toString());
      jsonContext.put("path", logEvent.getLoggerName());
      jsonContext.put("thread", logEvent.getThreadName());

      if (logEvent.getSource() != null) {
        jsonContext.put("class", logEvent.getSource().getClassName());
        jsonContext.put("file", logEvent.getSource().getFileName() + ":" + logEvent.getSource().getLineNumber());
        jsonContext.put("method", logEvent.getSource().getMethodName());
      }

      if (logEvent.getThrown() != null) {
        jsonContext.put("stack_trace", prettyPrintStackTrace(logEvent.getThrown()));
      }

      for (JsonLogEnhancer p : loggingProcessors) {
        p.map(processContext, jsonContext, alreadyEncodedJsonObjects);
      }

      String json = objectMapper.writeValueAsString(jsonContext);
      if (!alreadyEncodedJsonObjects.isEmpty()) {
        json = json.substring(0, json.length() - 1) + "," + String.join(",", alreadyEncodedJsonObjects) + "}";
      }

      if (prettyPrint) {
        json = objectMapper.writerWithDefaultPrettyPrinter()
          .writeValueAsString(objectMapper.readValue(json, Object.class)) + prettyPrintSuffix;
      }

      return (json + "\n").getBytes();
    } catch (Exception ex) {
      try {
        for (JsonLogEnhancer p : loggingProcessors) {
          p.failed(processContext, jsonContext, alreadyEncodedJsonObjects, ex);
        }
      } finally {
        ex.printStackTrace();
      }
    }

    return new byte[0];
  }

  private static String prettyPrintStackTrace(Throwable throwable) {
    String offset = "\n\t";
    StringBuilder result = new StringBuilder(throwable.getClass().getName() + ": " + throwable.getMessage());
    for (Object frame : throwable.getStackTrace()) {
      result.append(offset).append(frame);
    }

    Throwable cause = throwable.getCause();
    while (cause != null) {
      result.append(offset).append("Caused by: ").append(cause.getClass().getName()).append(": ").append(cause.getMessage());
      for (Object frame : cause.getStackTrace()) {
        result.append(offset).append(frame);
      }
      cause = cause.getCause();
    }

    return result.toString();
  }

  @Override
  public LogEvent toSerializable(LogEvent logEvent) {
    return logEvent;
  }

  @Override
  public String getContentType() {
    return "application/octet-stream";
  }

  @PluginFactory
  public static ConnectJsonLayout createLayout() {
    return new ConnectJsonLayout();
  }
}
