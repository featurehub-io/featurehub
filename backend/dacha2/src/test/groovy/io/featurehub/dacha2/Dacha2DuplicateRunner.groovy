package io.featurehub.dacha2

import bathe.BatheBooter
import org.junit.Test

class Dacha2DuplicateRunner {
  @Test
  void run() {
    new BatheBooter().run(new String[]{"-R" + Application.class.getName(),
      "-Pclasspath:/application2.properties",
      '-P${user.home}/.featurehub/fhos-common.properties',
      '-P${user.home}/.featurehub/dacha2.properties'})
  }

}
