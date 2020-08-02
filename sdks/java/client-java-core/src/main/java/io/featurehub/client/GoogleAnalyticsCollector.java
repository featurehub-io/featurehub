package io.featurehub.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

public class GoogleAnalyticsCollector implements AnalyticsCollector {
  private static final Logger log = LoggerFactory.getLogger(GoogleAnalyticsCollector.class);
  private final String uaKey; // this must be provided
  private final GoogleAnalyticsApiClient client;
  private String cid; // if this is null, we will look for it in "other" and log an error if it isn't there

  public GoogleAnalyticsCollector(String uaKey, String cid, GoogleAnalyticsApiClient client) {
    this.uaKey = uaKey;
    this.cid = cid;
    this.client = client;

    if (uaKey == null) {
      throw new RuntimeException("UA id must be provided when using the Google Analytics Collector.");
    }
    if (client == null) {
      throw new RuntimeException("Unable to log any events as there is no client, please configure one.");
    }
  }

  public void setCid(String cid) {
    this.cid = cid;
  }

  @Override
  public void logEvent(String action, Map<String, String> other, List<FeatureStateHolder> featureStateAtCurrentTime) {
    StringBuilder batchData = new StringBuilder();

    String finalCid = cid == null ? other.get("cid") : cid;

    if (finalCid == null) {
      log.error("There is no CID provided for GA, not logging any events.");
      return;
    }

    String ev = null;
    try {
      ev = (other != null && other.get(GoogleAnalyticsApiClient.GA_VALUE) != null)
        ? ("&ev=" + URLEncoder.encode(other.get(GoogleAnalyticsApiClient.GA_VALUE), StandardCharsets.UTF_8.name())) :
        "";

      String baseForEachLine =
        "v=1&tid=" + uaKey + "&cid=" + cid + "&t=event&ec=FeatureHub%20Event&ea=" + URLEncoder.encode(action,
          StandardCharsets.UTF_8.name()) + ev + "&el=";

      featureStateAtCurrentTime.forEach((fsh) -> {
        String line = null;
        if (fsh instanceof FeatureStateBooleanHolder) {
          line = fsh.getBoolean().equals(Boolean.TRUE) ? "on" : "off";
        } else if (fsh instanceof FeatureStateStringHolder) {
          line = fsh.getString();
        } else if (fsh instanceof FeatureStateNumberHolder) {
          line = fsh.getNumber().toPlainString();
        }
        if (line != null) {

          try {
            line = URLEncoder.encode(fsh.getKey() + " : " + line, StandardCharsets.UTF_8.name());
            batchData.append(baseForEachLine);
            batchData.append(line);
            batchData.append("\n");
          } catch (UnsupportedEncodingException e) { // can't happen
          }
        }
      });
    } catch (UnsupportedEncodingException e) { // can't happen
    }

    if (batchData.length() > 0) {
      client.postBatchUpdate(batchData.toString());
    }

  }
}
