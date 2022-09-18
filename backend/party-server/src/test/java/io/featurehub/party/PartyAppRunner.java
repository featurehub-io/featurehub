package io.featurehub.party;

import bathe.BatheBooter;
import org.junit.Test;

public class PartyAppRunner {
  @Test
  public void run() throws Exception {
    new BatheBooter().run(new String[]{"-R" + io.featurehub.party.Application.class.getName(),
      "-Pclasspath:/application.properties",
      "-P${user.home}/.featurehub/fhos-common.properties",
      "-P${user.home}/.featurehub/party.properties"});
  }
}
