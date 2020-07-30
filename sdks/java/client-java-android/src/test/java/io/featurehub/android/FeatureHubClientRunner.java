package io.featurehub.android;

import io.featurehub.client.ClientFeatureRepository;
import io.featurehub.client.FeatureRepository;
import org.junit.Test;

import java.io.IOException;
import java.util.Collections;

// isn't a test, won't run outside of IDE but will run with test dependencies
public class FeatureHubClientRunner {
  @Test
  public void test() throws IOException {
    FeatureRepository repo = new ClientFeatureRepository(5);
    repo.addReadynessListener(rl -> System.out.println("readyness " + rl.toString()));

    final FeatureHubClient client = new FeatureHubClient("http://localhost:8553",
      Collections.singleton("default/ce6b5f90-2a8a-4b29-b10f-7f1c98d878fe/VNftuX5LV6PoazPZsEEIBujM4OBqA1Iv9f9cBGho2LJylvxXMXKGxwD14xt2d7Ma3GHTsdsSO8DTvAYF")
      , repo);

    client.checkForUpdates();

    System.out.println("waiting");
    System.in.read();


  }
}
