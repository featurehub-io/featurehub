package io.featurehub.edge

import bathe.BatheBooter
import org.junit.Test

class EdgeGetRunner {
  @Test
  void run() throws Exception {
    new BatheBooter().run(new String[]{
      "-R" + Application.class.getName(),
      "-Pclasspath:/application.properties",
      '-P\${user.home}/.featurehub/edge-db.properties'});
  }
}
