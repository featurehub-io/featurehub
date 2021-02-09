package io.featurehub.android;

import io.featurehub.client.ClientFeatureRepository;
import io.featurehub.client.FeatureContext;
import io.featurehub.sse.model.StrategyAttributeDeviceName;
import io.featurehub.sse.model.StrategyAttributePlatformName;
import org.junit.Test;

import java.io.IOException;
import java.util.Collections;

// isn't a test, won't run outside of IDE but will run with test dependencies
public class FeatureHubClientRunner {
  @Test
  public void test() throws IOException {
    ClientFeatureRepository repo = new ClientFeatureRepository(5);
    repo.addReadynessListener(rl -> System.out.println("readyness " + rl.toString()));

    final FeatureHubClient client = new FeatureHubClient("http://localhost:8553",
      Collections.singleton("default/ec6a720b-71ac-4cc1-8da1-b5e396fa00ca/Kps0MAqsGt5QhgmwMEoRougAflM2b8Q9e1EFeBPHtuIF0azpcCXeeOw1DabFojYdXXr26fyycqjBt3pa")
      , repo);

    final FeatureContext ctx = new FeatureContext(repo, client);

    ctx.userKey("andrew")
      .platform(StrategyAttributePlatformName.ANDROID)
      .device(StrategyAttributeDeviceName.MOBILE)
      .attr("testapp", "android-pretend").build();

    System.out.println("waiting");
    System.in.read();
  }
}
