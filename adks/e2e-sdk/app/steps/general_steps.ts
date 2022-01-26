import { Given, Then } from '@cucumber/cucumber';
import { FeatureValueType, RolloutStrategy, RolloutStrategyAttribute } from 'featurehub-javascript-admin-sdk';
import { ClientContext } from 'featurehub-javascript-node-sdk';
import DataTable from '@cucumber/cucumber/lib/models/data_table';
import * as fs from 'fs';
import { SdkWorld } from '../support/world';
import { expect } from 'chai';
import waitForExpect from 'wait-for-expect';

Then(/^I add a strategy (.*) with (.*) percentage and value (.*)$/, async function (strategyName, percentage, value, table: DataTable) {
  const fValue = await this.getFeature();
  const rs = new RolloutStrategy({ name: strategyName });

  if (percentage !== 'no') {
    rs.percentage = parseInt(percentage);
  }

  if (value !== 'no' && value !== 'null') {
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

Then(/^I clear the context$/, function () {
  this.resetContext();
  console.log('context cleared');
});

Given(/^I connect to the Edge server using (sse-client-eval|poll-client-eval|poll-server-eval)$/, function () {

});

Then('I write out a feature-examples config file', function () {
  const buf = `#!/bin/sh\nexport FEATUREHUB_CLIENT_API_KEY=${this.sdkUrlClientEval}\nexport FEATUREHUB_EDGE_URL=${this.featureUrl}\nexport FEATUREHUB_BASE_URL=${this.adminUrl}\n`;

  fs.writeFileSync('./example-test.sh', buf);
});

Then(/^there are (\d+) features$/, async function (numberOfFeatures) {
  const world = this as SdkWorld;

  await waitForExpect(() => {
    console.log("number of features", world.repository.simpleFeatures().size);
    expect(numberOfFeatures).to.eq(world.repository.simpleFeatures().size);
  }, 4000, 300);
});

Then(/^I delete the feature$/, async function () {
  const world = this as SdkWorld;

  const features = await world.featureApi.deleteFeatureForApplication(world.application.id, world.feature.key);

  expect(features).to.not.be.undefined;
});
