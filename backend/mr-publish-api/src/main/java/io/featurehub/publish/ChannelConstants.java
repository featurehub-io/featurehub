package io.featurehub.publish;

public class ChannelConstants {
  // when Edge and the Cache communicate based on a named cache, they will
  // talk (based on spec) as /cache-id/edge_v1 - e.g. /default/edge_v1
  public static String EDGE_CACHE_CHANNEL = "edge_v2";

  public static String DEFAULT_CACHE_NAME = "default";
}
