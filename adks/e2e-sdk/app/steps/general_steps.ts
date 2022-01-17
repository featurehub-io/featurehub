import { Given, Then, When } from '@cucumber/cucumber';
import { Feature, FeatureValueType, RolloutStrategy, RolloutStrategyAttribute } from 'featurehub-javascript-admin-sdk';
import { makeid } from '../support/random';
import { expect } from 'chai';
import waitForExpect from 'wait-for-expect';
import { ClientContext, FeatureHubRepository, FeatureStateHolder, Readyness } from 'featurehub-javascript-node-sdk';
import DataTable from '@cucumber/cucumber/lib/models/data_table';

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

Given(/^I connect to the Edge server using (sse-client-eval|poll-client-eval|poll-server-eval)$/, function() {

});