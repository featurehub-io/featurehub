import {Given, Then, When} from "@cucumber/cucumber";
import {SdkWorld} from "../support/world";
import {expect} from "chai";
import {actualPercentage, validateWorldForApplicationStrategies} from "./strategies";
import {
  ApplicationRolloutStrategy,
  FeatureHistoryItem,
  FeatureHistoryList,
  FeatureHistoryOrder,
  RolloutStrategy,
  RolloutStrategyAttribute
} from "../apis/mr-service";
import {convertValue, extractRolloutStrategyFromDataTable} from "../support/utils";
import DataTable from "@cucumber/cucumber/lib/models/data_table";

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

export async function findHistory(world: SdkWorld) : Promise<FeatureHistoryItem> {
  const history = await getFeatureHistory(world);

  const historyItem = history.items.find(s => s.envId == world.environment.id && s.featureId == world.feature.id);
  expect(historyItem).to.not.be.undefined;

  return historyItem;
}

/**
 * The shared (application or portfolio) strategy definition, i.e. the rules that the feature history is expected
 * to show for a shared strategy attached to a feature value.
 */
export interface SharedStrategyDefinition {
  name?: string;
  percentage?: number | null;
  percentageAttributes?: Array<string> | null;
  attributes?: Array<RolloutStrategyAttribute> | null;
}

/**
 * The rollout strategies in the most recent version of the current feature value's history.
 */
async function latestHistoryStrategies(world: SdkWorld): Promise<Array<RolloutStrategy>> {
  const history =
    await world.featureHistoryApi.listFeatureHistory(world.application.id, [world.feature.key],
      [], [], [world.environment.id], 10, 0, FeatureHistoryOrder.Desc);

  expect(history.status).to.eq(200);
  expect(history.data.items.length).to.be.gt(0);
  expect(history.data.items[0].envId).to.eq(world.environment.id);
  expect(history.data.items[0].history.length).to.be.gt(0);
  const rolloutStrategies = history.data.items[0].history[0].rolloutStrategies;
  expect(rolloutStrategies.length).to.be.gt(0);
  return rolloutStrategies;
}

export async function findStrategyInLatestHistory(world: SdkWorld, uniqueCode: string): Promise<RolloutStrategy> {
  const rolloutStrategies = await latestHistoryStrategies(world);
  const found = rolloutStrategies.find(rs => rs.id === uniqueCode);
  expect(found, `strategy ${uniqueCode} is not in the latest feature history: ${JSON.stringify(rolloutStrategies)}`).to.not.be.undefined;
  return found;
}

function normaliseAttributes(attributes?: Array<RolloutStrategyAttribute> | null) {
  return (attributes ?? []).map(a => ({
    fieldName: a.fieldName,
    conditional: a.conditional,
    type: a.type,
    values: [...(a.values ?? [])].sort()
  }));
}

export function expectHistoryAttributes(found: RolloutStrategy, expected?: Array<RolloutStrategyAttribute> | null) {
  expect(normaliseAttributes(found.attributes),
    `feature history for strategy ${found.name} has the wrong rules`).to.deep.eq(normaliseAttributes(expected));
}

export function expectHistoryPercentage(found: RolloutStrategy, expected?: number | null) {
  expect(found.percentage ?? undefined,
    `feature history for strategy ${found.name} has the wrong percentage`).to.eq(expected ?? undefined);
}

function expectHistoryPercentageAttributes(found: RolloutStrategy, expected?: Array<string> | null) {
  expect([...(found.percentageAttributes ?? [])].sort(),
    `feature history for strategy ${found.name} has the wrong percentage attributes`)
    .to.deep.eq([...(expected ?? [])].sort());
}

/**
 * Checks the latest feature history has the shared strategy with the expected value, and that it carries the
 * rules of the shared strategy (attributes, percentage attributes) and its percentage - the per-feature-value
 * percentage override takes precedence over the shared strategy's own percentage.
 */
export async function validateFeatureHistory(world: SdkWorld, uniqueCode: string, definition: SharedStrategyDefinition,
                                             value: string, percentageOverride?: number | null) {
  const found = await findStrategyInLatestHistory(world, uniqueCode);

  expect(found.name).to.eq(definition.name);
  expect(found.value).to.eq(convertValue(value, world.feature.valueType));
  expectHistoryAttributes(found, definition.attributes);
  expectHistoryPercentageAttributes(found, definition.percentageAttributes);
  expectHistoryPercentage(found, percentageOverride ?? definition.percentage);
}

/**
 * The percentage override (if any) on the shared strategy instance attached to the current feature value.
 */
export async function attachedPercentageOverride(world: SdkWorld, strategyId: string, portfolio: boolean): Promise<number | undefined> {
  const featureValue = await world.getFeatureValue();
  const instances = portfolio ? featureValue.portfolioStrategyInstances : featureValue.rolloutStrategyInstances;
  const instance = instances?.find(rsi => rsi.strategyId === strategyId);
  expect(instance, `strategy ${strategyId} is not attached to the current feature value`).to.not.be.undefined;
  return instance.percentageOverride ?? undefined;
}

/**
 * rules tables have the columns fieldName | conditional | values | type (the same as the strategy creation tables)
 */
export function rulesFromDataTable(table: DataTable): Array<RolloutStrategyAttribute> {
  const strategies = extractRolloutStrategyFromDataTable(table);
  expect(strategies.length, 'No rules defined').to.eq(1);
  return strategies[0].attributes;
}

async function applicationStrategyDetails(world: SdkWorld, strategyKey: string): Promise<{uniqueCode: string, definition: ApplicationRolloutStrategy}> {
  const strategy = world.applicationStrategies[strategyKey];
  validateWorldForApplicationStrategies(world, strategy, strategyKey);

  const details = await world.currentUser.applicationStrategyApi.getApplicationStrategy(world.application.id, strategy.strategy.id);
  expect(details.status).to.eq(200);

  return { uniqueCode: strategy.uniqueCode, definition: details.data };
}

Given("I save the current feature value's history using the key {string}", async function (name: string) {
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

Then("I expect the application strategy {string} to be attached to the feature history with the value {string}", async function (strategyKey: string, value: string) {
  const world = this as SdkWorld;

  const {uniqueCode, definition} = await applicationStrategyDetails(world, strategyKey);
  const percentageOverride = await attachedPercentageOverride(world, definition.id, false);

  await validateFeatureHistory(world, uniqueCode, definition, value, percentageOverride);
});

Then("the feature history for application strategy {string} has the rules", async function (strategyKey: string, table: DataTable) {
  const world = this as SdkWorld;

  const {uniqueCode} = await applicationStrategyDetails(world, strategyKey);
  expectHistoryAttributes(await findStrategyInLatestHistory(world, uniqueCode), rulesFromDataTable(table));
});

Then("the feature history for application strategy {string} has percentage {int}", async function (strategyKey: string, percentage: number) {
  const world = this as SdkWorld;

  const {uniqueCode} = await applicationStrategyDetails(world, strategyKey);
  expectHistoryPercentage(await findStrategyInLatestHistory(world, uniqueCode), actualPercentage(percentage));
});

Then("the feature history for application strategy {string} has no percentage", async function (strategyKey: string) {
  const world = this as SdkWorld;

  const {uniqueCode} = await applicationStrategyDetails(world, strategyKey);
  expectHistoryPercentage(await findStrategyInLatestHistory(world, uniqueCode), undefined);
});

Then("the feature history saved as {string} still has the application strategy {string} with the rules", async function (name: string, strategyKey: string, table: DataTable) {
  const world = this as SdkWorld;

  const saved = world.featureHistorySave[name];
  expect(saved, `no feature history saved as ${name}`).to.not.be.undefined;

  const strategy = world.applicationStrategies[strategyKey];
  validateWorldForApplicationStrategies(world, strategy, strategyKey);

  // re-read the history so we see what the server now says about that version
  const historyItem = await findHistory(world);
  const version = historyItem.history.find(s => s.version == saved.version);
  expect(version, `version ${saved.version} is no longer in the feature history`).to.not.be.undefined;

  const found = version.rolloutStrategies.find(rs => rs.id === strategy.uniqueCode);
  expect(found, `strategy ${strategyKey} is not in feature history version ${saved.version}`).to.not.be.undefined;
  // archived strategies have a suffix added to their name, history should show the original name
  expect(found.name).to.eq(strategy.strategy.name);
  expectHistoryAttributes(found, rulesFromDataTable(table));
});

When("I archive the application strategy {string}", async function (strategyKey: string) {
  const world = this as SdkWorld;

  const strategy = world.applicationStrategies[strategyKey];
  validateWorldForApplicationStrategies(world, strategy, strategyKey);

  const result = await world.currentUser.applicationStrategyApi.deleteApplicationStrategy(world.application.id, strategy.strategy.id);
  expect(result.status).to.be.oneOf([200, 204]);
});
