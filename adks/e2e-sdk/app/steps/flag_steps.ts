import { Given, Then, When } from '@cucumber/cucumber';
import { Feature, FeatureValue, FeatureValueType, RolloutStrategy } from '../apis/mr-service';
import { makeid } from '../support/random';
import { expect } from 'chai';
import waitForExpect from 'wait-for-expect';
import { FeatureStateHolder, Readyness } from 'featurehub-javascript-node-sdk';
import { logger } from '../support/logging';
import { SdkWorld } from '../support/world';
import DataTable from '@cucumber/cucumber/lib/models/data_table';

Given(/^There is a new feature flag$/, async function () {
  const name = makeid(5).toUpperCase();
  const fCreate = await this.featureApi.createFeaturesForApplication(this.application.id, new Feature({
    name: name,
    key: name,
    valueType: FeatureValueType.Boolean
  }));
  expect(fCreate.status).to.eq(200);
  const feat = fCreate.data.find((f: Feature) => f.key == name);
  expect(feat).to.not.be.undefined;
  this.feature = feat;
});

When(/^I set the feature value to (.*)$/, async function(value: string) {
  const world = this as SdkWorld;

  expect(world.application.id).to.not.be.undefined;
  expect(world.environment.id).to.not.be.undefined;
  expect(world.feature.id).to.not.be.undefined;

  const result = await world.featureValueApi.createFeatureForEnvironment(world.environment.id, world.feature.key, new FeatureValue({
    key: world.feature.key,
    locked: false,
    valueNumber: world.feature.valueType === FeatureValueType.Number ? parseFloat(value) : null,
    valueBoolean: world.feature.valueType === FeatureValueType.Boolean ? ('true' === value) : null,
    valueJson: world.feature.valueType === FeatureValueType.Json ? value : null,
    valueString: world.feature.valueType === FeatureValueType.String ? value : null,
  }));
  expect(result.status).to.eq(200);
});

Given(/^There is a feature (flag|string|number|json) with the key (.*)$/, async function (type: string, key: string) {
  let fType: FeatureValueType | undefined;
  if (type === "flag" || type === "boolean") {
    fType = FeatureValueType.Boolean;
  } else if (type === "string") {
    fType = FeatureValueType.String;
  } else if (type === "number") {
    fType = FeatureValueType.Number;
  } else if (type === "json") {
    fType = FeatureValueType.Json;
  }
  expect(fType, `${type} is an unrecognized flag type`).to.not.be.undefined;
  const fCreate = await this.featureApi.createFeaturesForApplication(this.application.id, new Feature({
    name: key,
    key: key,
    valueType: fType
  }));
  expect(fCreate.status).to.eq(200);
  const feature = fCreate.data.find((f: Feature) => f.key == key);
  expect(feature).to.not.be.undefined;

  this.feature = feature;
});

Then(/^the feature flag is (locked|unlocked) and (off|on)$/, async function (lockedStatus, value) {
  const world = this as SdkWorld;
  await waitForExpect(() => {
    expect(world.repository).to.not.be.undefined;
    expect(world.repository.readyness).to.eq(Readyness.Ready);
  }, 2000, 500);

  await waitForExpect(() => {
    // const f = this.featureState(this.feature.key) as FeatureStateHolder;
    const f = this.featureState(world.feature.key) as FeatureStateHolder;
    console.log('key is val', world.feature.key, f.getBoolean(), value, f.isLocked(), lockedStatus);
    logger.info('the feature %s is value %s and locked status %s', this.feature.key, f.getBoolean(), f.locked);
    expect(f.getBoolean(), `${f.key} flag is >${f.flag}< and expected to have a value >${value == 'on'}<`).to.eq(value === 'on');
    expect(f.isLocked(), `${f.key} locked ${f.locked} and expected ${lockedStatus}`).to.eq(lockedStatus === 'locked');
  }, 4000, 500);
});

When(/^I (unlock|lock) the feature$/, async function (lockUnlock) {
  const fValue = await (this as SdkWorld).getFeatureValue();
  fValue.locked = (lockUnlock === 'lock');
  await this.updateFeature(fValue);
});

Then(/^I set the feature flag to (on|off|locked|unlocked)$/, async function (flagChange) {
  const fValue = await (this as SdkWorld).getFeatureValue();
  if (flagChange === 'on' || flagChange === 'off') {
    fValue.valueBoolean = (flagChange === 'on');
  } else {
    fValue.locked = (flagChange === 'locked');
  }

  fValue.whenUpdated = undefined;
  fValue.whoUpdated = undefined;
  await this.updateFeature(fValue);
});

Then(/^I set the feature flag to (on|off|locked|unlocked) and (on|off|locked|unlocked)$/, async function (flagChange1, flagChange2) {
  const fValue = await (this as SdkWorld).getFeatureValue();
  if (flagChange1 === 'on' || flagChange1 === 'off') {
    fValue.valueBoolean = (flagChange1 === 'on');
  } else {
    fValue.locked = (flagChange1 === 'locked');
  }
  if (flagChange2 === 'on' || flagChange2 === 'off') {
    fValue.valueBoolean = (flagChange2 === 'on');
  } else {
    fValue.locked = (flagChange2 === 'locked');
  }
  await this.updateFeature(fValue);
});


When(/^I setup (\d+) random feature (flags|strings|numbers|json)$/, async function (counter: number, type: string) {
  let valueType = FeatureValueType.Boolean;

  if (type === 'strings') {
    valueType = FeatureValueType.String;
  }
  if (type === 'numbers') {
    valueType = FeatureValueType.Number;
  }
  if (type === 'json') {
    valueType = FeatureValueType.Json;
  }

  for (let pos = 0; pos < counter; pos ++) {
    const key = makeid(10);

    const fCreate = await this.featureApi.createFeaturesForApplication(this.application.id, new Feature({
      name: key,
      key: key,
      valueType: valueType
    }));

    expect(fCreate.status).to.eq(200);
  }

});

When(/^I create custom rollout strategies$/, async function (table: DataTable) {
  const world = this as SdkWorld;
  const fv = (await world.featureValueApi.getFeatureForEnvironment(world.environment.id, world.feature.key)).data;

  fv.rolloutStrategies = [];

  for(const row of table.hashes()) {
    console.log('strategy row', row);
    const percentage = parseInt(row['percentage']);
    const name = row['name'];
    const value = row['value'];
    const rs = new RolloutStrategy({
      name: name,
      percentage: percentage,
      value: value === 'on'
    });
    fv.rolloutStrategies.push(rs);
  }

  const updated = await world.updateFeature(fv);
  updated.rolloutStrategies.forEach(rs => rs.id = undefined);
  expect(strategyComparison(updated.rolloutStrategies, fv.rolloutStrategies)).to.be.true;
});

function strategyComparison(one: Array<RolloutStrategy>, two: Array<RolloutStrategy>): boolean {
  if (one.length != two.length) return false;
  for(let count = 0; count < one.length; count ++) {
    if ((one[count].percentage !== two[count].percentage) ||
       (one[count].value !== two[count].value) ||
      (one[count].name !== two[count].name)) {
      console.log('one: ', JSON.stringify(one[count]), 'not equal  to two: ', JSON.stringify(two[count]));
      return false;
    }
  }
  return true;
}

function decodeStrategies(s: string | undefined): Array<RolloutStrategy> {
  if (!s || s.trim().length == 0) return [];

  const strategies: Array<RolloutStrategy> = [];
  s.split(",").forEach(strat => {
    const data = strat.split("/");
    strategies.push(new RolloutStrategy({
      name: data[1],
      percentage: parseInt(data[0]),
      value: data[2] === 'on'
    }));
  });

  return strategies;
}
When(/^I check the feature history I see$/, async function (table: DataTable) {
  const world = this as SdkWorld;

  const response = await world.historyApi.listFeatureHistory(world.application.id, [], [world.feature.id], [],
      [world.environment.id]);

  expect(response.status).to.eq(200);
  const data = response.data;
  expect(data.items.length).to.eq(1);
  expect(data.items[0].envId).to.eq(world.environment.id);
  expect(data.items[0].featureId).to.eq(world.feature.id);
  const itemData = data.items[0];

  // wip the ids as we can't compare those
  itemData.history.forEach(it => {
    it.rolloutStrategies?.forEach(rs => rs.id = undefined);
  });

  let pos = 0; // position in version history

  for (const row of table.hashes()) {
    console.log('history row', row);
    const locked = row['locked'] === 'true';
    const value = row['value'] === 'on';
    const retired = row['retired'] === 'true';
    const strategies = decodeStrategies(row['strategies']);
    let found = false;
    while (!found && pos < itemData.history.length) {
      console.log('comparing ', JSON.stringify(itemData.history[pos]), 'to locked', locked, 'retired', retired, 'value', value, 'strategies', JSON.stringify(strategies));
      const item = itemData.history[pos++];
      found = (item.locked == locked) && (item.retired == retired) && (item.value == value) && (strategyComparison(item.rolloutStrategies, strategies));
    }

    expect(found).to.be.true;
  }
});
