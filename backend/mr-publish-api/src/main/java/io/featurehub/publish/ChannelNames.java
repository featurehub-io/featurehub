package io.featurehub.publish;

public class ChannelNames {
  private static final String featureValueChannelName = "feature-updates";
  public static final String environmentChannelName = "environment-updates";
  public static final String serviceAccountChannelName = "service-account-channel";
  public static final String managementChannelName = "cache-management";

  public static String featureValueChannel(String cacheName) {
    return cache(cacheName, featureValueChannelName);
  }
  public static String serviceAccountChannel(String cacheName) { return cache(cacheName, serviceAccountChannelName); }
  public static String environmentChannel(String cacheName) { return cache(cacheName, environmentChannelName); }
  public static String managementChannel(String cacheName) { return cache(cacheName, managementChannelName); }

  public static String cache(String cacheName, String channelName) {
    return cacheName + "/" + channelName;
  }
}
