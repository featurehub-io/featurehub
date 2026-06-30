import {Given, Then, When} from "@cucumber/cucumber";
import {CreatePortfolioRolloutStrategy, FeatureHistoryOrder, RolloutStrategyInstance} from "../apis/mr-service";
import {SdkWorld} from "../support/world";
import {expect} from "chai";
import {compareStrategies, convertValue, extractRolloutStrategyFromDataTable} from "../support/utils";
import waitForExpect from "wait-for-expect";
import {FeatureStateBaseHolder, Readyness} from "featurehub-javascript-node-sdk";
import {findHistory} from "./feature-history";
import DataTable from "@cucumber/cucumber/lib/models/data_table";


Given("I create portfolio strategies", async function (table: DataTable) {
  const world = this as SdkWorld;

  expect(world.portfolio, 'You must have a portfolio to create an portfolio strategy').to.not.be.undefined;

  const rs = extractRolloutStrategyFromDataTable(table);
  expect(rs.length, 'No strategies defined').to.be.gt(0);

  for(const strat of rs) {
    const strategy = new CreatePortfolioRolloutStrategy({
      name: strat.name,
      percentage: strat.percentage,
      percentageAttributes: strat.percentageAttributes,
      attributes: strat.attributes
    });

    const result = await world.currentUser.portfolioStrategyApi.createPortfolioStrategy(world.portfolio.id, strategy);

    expect(result.status).to.eq(201);
  }
  const data = await world.currentUser.portfolioStrategyApi.listPortfolioStrategies(world.portfolio.id);
  expect(data.status).to.eq(200);
  expect(data.data.max).to.be.gt(0);
  for(const strat of rs) {
    const strategy = data.data.items.find(s => s.strategy.name == strat.name);
    expect(strategy).to.not.be.undefined;
    const details = await world.currentUser.portfolioStrategyApi.getPortfolioStrategy(world.portfolio.id, strategy.strategy.id);
    expect(details.status).to.eq(200);
    compareStrategies(details.data, strat);
  }
});

async function findStrategy(world: SdkWorld, name: string, includeUsage = false) {
  const data = await world.currentUser.portfolioStrategyApi.listPortfolioStrategies(world.portfolio.id, false, includeUsage, false, name);
  expect(data.status).to.eq(200);
  expect(data.data.items.length).to.eq(1);
  return data.data.items[0];
}

When("I attach portfolio strategy {string} to the current environment feature value with the value {string}", async function(strategyName: string, value: string) {
  const world = this as SdkWorld;
  const strategy = await findStrategy(world, strategyName);

  const featureValue = await world.getFeatureValue();

  const v = convertValue(value, world.feature.valueType);
  featureValue.portfolioStrategyInstances.push(new RolloutStrategyInstance({ strategyId: strategy.strategy.id,
    value:  v}));

  const updatedValue = await world.updateFeature(featureValue);
  expect(updatedValue.portfolioStrategyInstances.find(rsi =>
    rsi.strategyId === strategy.strategy.id && rsi.value === v )).to.not.be.undefined;
});

export async function validateFeatureHistory(world: SdkWorld, uniqueCode: string, strategyName: string, value: string) {
  const history =
    await world.featureHistoryApi.listFeatureHistory(world.application.id, [world.feature.key],
      [], [], [world.environment.id], 10, 0, FeatureHistoryOrder.Desc);

  expect(history.status).to.eq(200);
  expect(history.data.items.length).to.be.gt(0);
  expect(history.data.items[0].envId).to.eq(world.environment.id);
  expect(history.data.items[0].history.length).to.be.gt(0);
  let rolloutStrategies = history.data.items[0].history[0].rolloutStrategies;
  expect(rolloutStrategies.length).to.be.gt(0);

  const found = rolloutStrategies.find(rs => rs.id === uniqueCode)
  expect(found).to.not.be.undefined;
  expect(found.name).to.eq(strategyName);
  expect(found.value).to.eq(convertValue(value, world.feature.valueType));
}

Then('I expect the portfolio strategy {string} to be attached to the feature history with the value set to {string}', async function (strategyName: string, value: string) {
  const world = this as SdkWorld;

  const strategy = await findStrategy(world, strategyName);

  await validateFeatureHistory(world, strategy.uniqueCode, strategy.strategy.name, value);
});

Then('the portfolio strategy {string} should be used in {int} environment with {int} feature', async function(strategyName: string, envCount: number, featureCount: number) {
  const world = this as SdkWorld;

  const strategy = await findStrategy(world, strategyName, true);

  expect(strategy.usage.length).to.eq(envCount);
  expect(strategy.usage[0].featuresCount).to.eq(featureCount);
});

Then('the feature flag has an portfolio strategy {string} which has a value of {string}', async function (strategyName: string, value: string) {
  const world = this as SdkWorld;
  await waitForExpect(() => {
    expect(world.repository).to.not.be.undefined;
    expect(world.repository.readyness).to.eq(Readyness.Ready);
  }, 2000, 500);

  const strategy = await findStrategy(world, strategyName, true);

  await waitForExpect(() => {
    // const f = this.featureState(this.feature.key) as FeatureStateHolder;
    const f = this.featureState(world.feature.key) as FeatureStateBaseHolder;
    const featureState = f.getFeatureState();
    const strategyFound = featureState.strategies.find(s => s.id === strategy.uniqueCode);
    expect(strategyFound, `strategy ${strategy.strategy.name} with id ${strategy.uniqueCode} was not found in ${JSON.stringify(featureState.strategies, null, 2)}`).to.not.be.undefined;
    expect(strategyFound.value, `strategy ${JSON.stringify(strategyFound, null, 2)} should have value of type bool`).to.be.a('boolean');
    expect(strategyFound.value).to.eq("true" === value);
  }, 4000, 500);
});

Then('there is an portfolio strategy called {string} in the current environment feature value', async function (strategyName: string) {
  const world = this as SdkWorld;

  const strategy = await findStrategy(world, strategyName);

  const featureValue = await world.getFeatureValue();

  expect(featureValue.portfolioStrategyInstances.find(rsi =>
    rsi.strategyId === strategy.strategy.id && rsi.value)).to.not.be.undefined;
});

When('I swap the order of portfolio strategies {string} and {string} they remain swapped', async function (key1: string, key2: string) {
  const world = this as SdkWorld;

  const strategy1 = await findStrategy(world, key1);
  const strategy2 = await findStrategy(world, key2);

  const featureValue = await world.getFeatureValue();
  const key1Index = featureValue.portfolioStrategyInstances.findIndex(s => s.strategyId == strategy1.strategy.id);
  expect(key1Index, `cannot find strategy ${key1} in strategies`).to.not.eq(-1);
  const key2Index= featureValue.portfolioStrategyInstances.findIndex(s => s.strategyId == strategy2.strategy.id);
  expect(key2Index, `cannot find strategy ${key2} in strategies`).to.not.eq(-1);

  const rsi = featureValue.portfolioStrategyInstances[key1Index];
  featureValue.portfolioStrategyInstances[key1Index] = featureValue.portfolioStrategyInstances[key2Index];
  featureValue.portfolioStrategyInstances[key2Index] = rsi;

  const updatedValue = await world.updateFeature(featureValue);
  expect(updatedValue.portfolioStrategyInstances[key1Index].strategyId, `Strategy 2 did not swap`).to.eq(strategy2.strategy.id);
  expect(updatedValue.portfolioStrategyInstances[key2Index].strategyId).to.eq(strategy1.strategy.id);
});

When('I delete the portfolio strategy called {string} from the current environment feature value', async function (strategyName: string) {
  const world = this as SdkWorld;

  const strategy = await findStrategy(world, strategyName);

  const featureValue = await world.getFeatureValue();
  featureValue.portfolioStrategyInstances = featureValue.portfolioStrategyInstances.filter(rsi => rsi.strategyId != strategy.strategy.id);
  const updatedValue = await world.updateFeature(featureValue);
  expect(updatedValue.portfolioStrategyInstances.find(rsi =>
    rsi.strategyId === strategy.strategy.id)).to.be.undefined;
});

When('I expect the portfolio strategy {string} to be removed from the feature history', async function (strategyName: string) {
  const world = this as SdkWorld;

  const strategy = await findStrategy(world, strategyName);

  const historyItem = await findHistory(world);
  const currentStatus = historyItem.history[historyItem.history.length-1];
  expect(currentStatus.rolloutStrategies.find(s => s.id === strategy.uniqueCode)).to.be.undefined;
});
