package io.featurehub.dacha;

import bathe.BatheBooter;
import org.junit.Test;

public class Dacha2AppRunner {
  @Test
  public void run() throws Exception {
    new BatheBooter().run(new String[]{"-R" + Application.class.getName(), "-Pclasspath:/application2.properties",
      "-P${user.home}/.featurehub/dacha.properties"});
  }
}
