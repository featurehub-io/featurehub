package io.featurehub.dacha2

import bathe.BatheBooter
import org.junit.Test

class Dacha2Runner {
  @Test
  void run() {
    new BatheBooter().run(new String[]{"-R" + Application.class.getName(),
      "-Pclasspath:/application.properties", '-P${user.home}/.featurehub/dacha2.properties'})
  }
}
