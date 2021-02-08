package io.featurehub.client.jersey;

import io.featurehub.client.ClientContext;
import io.featurehub.client.ClientFeatureRepository;
import io.featurehub.client.EdgeFeatureHubConfig;
import io.featurehub.client.Feature;
import io.featurehub.client.FeatureContext;
import io.featurehub.client.FeatureRepository;
import io.featurehub.sse.model.FeatureStateUpdate;
import io.featurehub.sse.model.StrategyAttributeDeviceName;
import io.featurehub.sse.model.StrategyAttributePlatformName;
import org.junit.Test;

enum Features implements Feature {
  FEATURE_SAMPLE, feature_json, feature_number, NEW_BOAT, NEW_BUTTON, SHOULDNT_exist;
}

public class JerseyClientSample {
  @Test
  public void clientTest() throws InterruptedException {
    final FeatureContext ctx = new FeatureContext(new EdgeFeatureHubConfig("ttp://localhost:8553/",
      "ec6a720b-71ac-4cc1-8da1-b5e396fa00ca" +
        "/Kps0MAqsGt5QhgmwMEoRougAflM2b8Q9e1EFeBPHtuIF0azpcCXeeOw1DabFojYdXXr26fyycqjBt3pa"));

    FeatureRepository cfr = ctx.getRepository();

    ctx.userKey("jimbob")
      .platform(StrategyAttributePlatformName.MACOS)
      .device(StrategyAttributeDeviceName.DESKTOP)
      .attr("testapp", "sample,a%%").build();

    Thread.sleep(5000);

    System.out.println("exists tests");
    for(Features f : Features.values()) {
      System.out.println(f.name() + ": " + ctx.exists(f));
    }
    System.out.println("set tests");
    for(Features f : Features.values()) {
      System.out.println(f.name() + ": " + cfr.getFeatureState(f).isSet());
    }

    System.out.println("active tests");
    for(Features f : Features.values()) {
      System.out.println(f.name() + ": " + cfr.getFeatureState(f).getBoolean());
    }

    for(Features f : Features.values()) {
      Features feat = f;
      cfr.getFeatureState(feat.name()).addListener((featureState) -> {
        System.out.print("feature " + feat.name() + " has changed to ");
        if (feat == Features.NEW_BOAT || feat == Features.NEW_BUTTON || feat == Features.FEATURE_SAMPLE) {
          System.out.println(featureState.getBoolean());
        }
        if (feat == Features.feature_json) {
          System.out.println(featureState.getRawJson());
        }
        if (feat == Features.feature_number) {
          System.out.println(featureState.getNumber());
        }
      });
    }

    Thread.currentThread().join();

  }

  private final static String changeToggleEnv = "default/fc5b929b-8296-4920-91ef-6e5b58b499b9" +
    "/VNftuX5LV6PoazPZsEEIBujM4OBqA1Iv9f9cBGho2LJylvxXMXKGxwD14xt2d7Ma3GHTsdsSO8DTvAYF";

  @Test
  public void changeToggleTest() {
    ClientFeatureRepository cfr = new ClientFeatureRepository(5);
    final JerseyClient client =
      new JerseyClient(new EdgeFeatureHubConfig("http://localhost:8553", changeToggleEnv), false,
        cfr, null);

    client.setFeatureState("NEW_BOAT", new FeatureStateUpdate().lock(false).value(Boolean.TRUE));
  }
}
