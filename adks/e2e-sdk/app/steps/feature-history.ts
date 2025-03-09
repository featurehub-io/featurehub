import {Given, Then} from "@cucumber/cucumber";
import {SdkWorld} from "../support/world";
import {expect} from "chai";
import {validateWorldForApplicationStrategies} from "./strategies";
import {FeatureHistoryItem, FeatureHistoryList, FeatureHistoryOrder} from "../apis/mr-service";

async function getFeatureHistory(world: SdkWorld, max: number = 10)  : Promise<FeatureHistoryList> {
  const history =
    await world.featureHistoryApi.listFeatureHistory(world.application.id, [world.feature.key],
      [], [], [world.environment.id], max, 1, FeatureHistoryOrder.Desc);

  expect(history.status).to.eq(200);

  world.featureHistory = history.data;

  return history.data;
}

Given("I get the feature history", async function () {
  const world = this as SdkWorld;
  await getFeatureHistory(world);
});

async function findHistory(world: SdkWorld) : Promise<FeatureHistoryItem> {
  const history = await getFeatureHistory(world);

  const historyItem = history.items.find(s => s.envId == world.environment.id && s.featureId == world.feature.id);
  expect(historyItem).to.not.be.undefined;

  return historyItem;
}

Given("I save this spot in feature history as {string} for application strategy {string}", async function (name: string, strategyKey: string) {
  const world = this as SdkWorld;

  const historyItem = await findHistory(world);

  world.featureHistorySave[name] = historyItem.history[historyItem.history.length-1];
});

Then("I expect the application strategy {string} to be removed from the feature history", async function (strategyKey: string) {
  const world = this as SdkWorld;

  const strategy = world.applicationStrategies[strategyKey];
  validateWorldForApplicationStrategies(world, strategy, strategyKey);

  const historyItem = await findHistory(world);
  const currentStatus = historyItem.history[historyItem.history.length-1];
  expect(currentStatus.rolloutStrategies.find(s => s.id === strategy.uniqueCode)).to.be.undefined;
});

Then("I find the feature history and compare it to {string} and it is the same", async function(name: string) {
  const world = this as SdkWorld;

  const saved = world.featureHistorySave[name];
  expect(saved).to.not.be.undefined;

  const historyItem = await findHistory(world);

  const found = historyItem.history.find(s => s.version == saved.version);
  expect(found).to.not.be.undefined;

  expect(found).to.deep.eq(saved);
});

Then("I expect the application strategy {string} to be attached to the feature history", async function (strategyKey: string) {
  const world = this as SdkWorld;

  const strategy = world.applicationStrategies[strategyKey];
  validateWorldForApplicationStrategies(world, strategy, strategyKey);

  const history =
    await world.featureHistoryApi.listFeatureHistory(world.application.id, [world.feature.key],
      [], [], [world.environment.id], 10, 1, FeatureHistoryOrder.Desc);

  expect(history.status).to.eq(200);
  expect(history.data.items.length).to.be.gt(0);
  expect(history.data.items[0].envId).to.eq(world.environment.id);
  expect(history.data.items[0].history.length).to.be.gt(0);
  let rolloutStrategies = history.data.items[0].history[0].rolloutStrategies;
  expect(rolloutStrategies.length).to.be.gt(0);
  expect(rolloutStrategies[rolloutStrategies.length-1].id).to.eq(strategy.uniqueCode);
  expect(rolloutStrategies[rolloutStrategies.length-1].name).to.eq(strategy.strategy.name);
  expect(rolloutStrategies[rolloutStrategies.length-1].value).to.eq(true);
});
