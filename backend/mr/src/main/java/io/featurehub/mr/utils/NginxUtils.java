package io.featurehub.mr.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class NginxUtils {
  private static final Logger log = LoggerFactory.getLogger(NginxUtils.class);

  // get around the issue of running nginx
  public static void seeIfWeNeedToRunNginx() {
    if (System.getProperty("run.nginx", System.getenv("RUN.NGINX")) != null) {
      try {
        new ProcessBuilder("/usr/sbin/nginx").start();
      } catch (IOException e) {
        log.error("Requested to start nginx but failed", e);
      }
    }
  }
}
