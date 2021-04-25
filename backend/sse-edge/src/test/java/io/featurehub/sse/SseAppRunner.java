package io.featurehub.sse;

import bathe.BatheBooter;
import io.featurehub.edge.Application;
import org.junit.Test;

public class SseAppRunner {
  @Test
  public void run() throws Exception {
    new BatheBooter().run(new String[]{"-R" + Application.class.getName(), "-Pclasspath:/application.properties",
      "-P${user.home}/.featurehub/edge.properties"});
  }
}
