package io.featurehub.mr;

import bathe.BatheBooter;
import io.featurehub.Application;
import org.junit.Test;

public class AppRunner {
  @Test
  public void run() throws Exception {
    new BatheBooter().run(new String[]{"-R" + Application.class.getName(), "-Pclasspath:/application.properties",
      "-P${user.home}/.featurehub/fhos-common.properties",
      "-P${user.home}/.featurehub/mr.properties"});
  }
}
