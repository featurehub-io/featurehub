import { Given, Then, When } from '@cucumber/cucumber';
import { Feature, FeatureValueType, RolloutStrategy, RolloutStrategyAttribute } from 'featurehub-javascript-admin-sdk';
import { makeid } from '../support/random';
import { expect } from 'chai';
import waitForExpect from 'wait-for-expect';
import { ClientContext, FeatureHubRepository, FeatureStateHolder, Readyness } from 'featurehub-javascript-node-sdk';
import DataTable from '@cucumber/cucumber/lib/models/data_table';

Given(/^There is a new feature flag$/, async function () {
  const name = makeid(5).toUpperCase();
  const fCreate = await this.featureApi.createFeaturesForApplication(this.application.id, new Feature({
    name: name,
    key: name,
    valueType: FeatureValueType.Boolean
  }));
  expect(fCreate.status).to.eq(200);
  expect(fCreate.data.length).to.eq(1);
  this.feature = fCreate.data[0];
});

Then(/^the feature flag is (locked|unlocked) and (off|on)$/, async function (lockedStatus, value) {
  await waitForExpect(() => {
    expect(this.repository.readyness).to.eq(Readyness.Ready);
    const f = this.featureState(this.feature.key) as FeatureStateHolder;
    // console.log('key is val', this.feature.key, f.getBoolean(), value, f.isLocked(), lockedStatus);
    // logger.info('the feature %s is value %s and locked status %s', this.feature.key, f.valueBoolean, f.locked);
    expect(f.getBoolean()).to.eq(value === 'on');
    expect(f.isLocked()).to.eq(lockedStatus === 'locked');
  }, 2000, 200);
});

When(/^I (unlock|lock) the feature$/, async function (lockUnlock) {
  const fValue = await this.getFeature();
  fValue.locked = (lockUnlock === 'lock');
  await this.updateFeature(fValue);
});

Then(/^I set the feature flag to (on|off|locked|unlocked)$/, async function (flagChange) {
  const fValue = await this.getFeature();
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
  const fValue = await this.getFeature();
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

Then(/^I add a strategy (.*) with (.*) percentage and value (.*)$/, async function (strategyName, percentage, value, table: DataTable) {
  const fValue = await this.getFeature();
  const rs = new RolloutStrategy({ name: strategyName });

  if (percentage !== 'no') {
    rs.percentage = parseInt(percentage);
  }

  if (value !== 'no') {
    switch (this.feature.valueType) {
      case FeatureValueType.Boolean:
        rs.value = (value === 'on');
        break;
      case FeatureValueType.Number:
        rs.value = parseFloat(value);
        break;
      default:
        rs.value = value;
        break;
    }
  }

  rs.attributes = [];

  for (const row of table.hashes()) {
    console.log('row is ', row);
    const rsa = new RolloutStrategyAttribute({
      fieldName: row['Field'],
      conditional: row['Conditional'],
      values: row['Values'].split(','),
      type: row['Type']
    });

    rs.attributes.push(rsa);
  }

  if (!fValue.rolloutStrategies) {
    fValue.rolloutStrategies = [];
  }
  fValue.rolloutStrategies.push(rs);

  await this.updateFeature(fValue);
});

Then(/^I set the context to$/, function (table) {
  this.resetContext();

  const ctx = this.context as ClientContext;

  table.hashes().forEach(row => {
    console.log('setting context', row);
    ctx.attribute_value(row['Field'], row['Value']);
  });
});

Then(/^I clear the context$/, function (table) {
  this.resetContext();

});


