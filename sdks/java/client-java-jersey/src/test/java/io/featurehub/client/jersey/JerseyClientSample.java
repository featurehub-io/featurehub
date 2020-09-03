package io.featurehub.client.jersey;

import io.featurehub.client.ClientFeatureRepository;
import io.featurehub.client.Feature;
import io.featurehub.client.StaticFeatureContext;
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
    ClientFeatureRepository cfr = new ClientFeatureRepository(5);

//    cfr.clientContext().userKey("jimbob")
//      .platform(StrategyAttributePlatformName.MACOS)
//      .device(StrategyAttributeDeviceName.DESKTOP)
//      .attr("testapp", "sample-app").build();

    new JerseyClient("http://localhost:8553/features/default/ec6a720b-71ac-4cc1-8da1-b5e396fa00ca/Kps0MAqsGt5QhgmwMEoRougAflM2b8Q9e1EFeBPHtuIF0azpcCXeeOw1DabFojYdXXr26fyycqjBt3pa", true, cfr);
    Thread.sleep(5000);

    StaticFeatureContext.repository = cfr;
    StaticFeatureContext ctx = StaticFeatureContext.getInstance();

    System.out.println("exists tests");
    for(Features f : Features.values()) {
      System.out.println(f.name() + ": " + ctx.exists(f));
    }
    System.out.println("set tests");
    for(Features f : Features.values()) {
      System.out.println(f.name() + ": " + ctx.isSet(f));
    }

    System.out.println("active tests");
    for(Features f : Features.values()) {
      System.out.println(f.name() + ": " + ctx.isActive(f));
    }

    for(Features f : Features.values()) {
      Features feat = f;
      ctx.addListener(feat, (featureState) -> {
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
    final JerseyClient client = new JerseyClient("http://localhost:8553/features/" + changeToggleEnv, false, cfr);

    client.setFeatureState("NEW_BOAT", new FeatureStateUpdate().lock(false).value(Boolean.TRUE));
  }
}
