import {Then, When} from "@cucumber/cucumber";
import {SdkWorld} from "../support/world";
import {expect} from "chai";

When('I ask for the feature dashboard', async function() {
  const world = this as SdkWorld;
  expect(world.application.environments.length, `Application ${world.application.name} has no environments`).to.be.gt(0);
  const data = await world.featureApi.findAllFeatureAndFeatureValuesForEnvironmentsByApplication(world.application.id, world.application.environments.map(e => e.id));
  expect(data.status).to.eq(200);
  world.dashboard = data.data;
});

Then('I should see an empty list of features and {int} environment', async function (envCount: number) {
  const world = this as SdkWorld;
  expect(world.dashboard.environments.length).to.eq(1);
  expect(world.dashboard.features.length).to.eq(0);
});
