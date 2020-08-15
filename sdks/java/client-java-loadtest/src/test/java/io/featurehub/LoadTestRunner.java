package io.featurehub;

import bathe.BatheBooter;
import io.featurehub.loadtest.LoadTest;
import org.junit.Test;


import bathe.BatheBooter;
import org.junit.Test;

public class LoadTestRunner {
  @Test
  public void run() throws Exception {
    new BatheBooter().run(new String[]{"-R" + LoadTest.class.getName(),
      "-Pclasspath:/load-test.properties", "-P$" + "{user" + ".home}/.featurehub/load-test.properties"});
  }
}


