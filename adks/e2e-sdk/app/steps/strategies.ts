import {Given, Then, When} from '@cucumber/cucumber';
import { SdkWorld } from '../support/world';
import DataTable from '@cucumber/cucumber/lib/models/data_table';
import {CreateApplicationRolloutStrategy, RolloutStrategy, RolloutStrategyInstance} from '../apis/mr-service';
import waitForExpect from 'wait-for-expect';
import { expect } from 'chai';
import {makeid} from "../support/random";

Then(/^I (cannot|can) create custom flag rollout strategies$/, async function(can: string, table: DataTable) {
  const world = this as SdkWorld;

  const fv = await world.getFeatureValue();
  const s: Array<RolloutStrategy> = [];
  for(const row of table.hashes()) {
    const rs = new RolloutStrategy({
      percentage: parseFloat(row['percentage']),
      name: row['name'],
      value: row['value'] === 'true'
    });

    s.push(rs);
  }

  fv.rolloutStrategies = s;
  try {
    await world.updateFeature(fv, can === 'cannot' ? 422 : 200);
  } catch (e) {
    console.log('error is', e);
  }
});

Given('I have an application strategy', async function() {
  const world = this as SdkWorld;

  expect(world.application, 'You must have an application to create an application strategy').to.not.be.undefined;

  const data = await world.applicationStrategyApi.createApplicationStrategy(world.application.id, new CreateApplicationRolloutStrategy({
    name: makeid(10), disabled: false, attributes: []
  }));
  expect(data.status).to.eq(201);
  world.applicationStrategy = data.data;
});

When('I attach the application strategy to the current environment feature value', async function () {
  const world = this as SdkWorld;

  expect(world.applicationStrategy).to.not.be.undefined;
  expect(world.environment).to.not.be.undefined;
  expect(world.feature).to.not.be.undefined;

  const featureValue = await world.getFeatureValue();

  featureValue.rolloutStrategyInstances.push(new RolloutStrategyInstance({ strategyId: world.applicationStrategy.id,
    value: true }));

  const updatedValue = await world.updateFeature(featureValue);
  expect(updatedValue.rolloutStrategyInstances.find(rsi =>
    rsi.strategyId === world.applicationStrategy.id && rsi.value)).to.not.be.undefined;
});

Then("the edge repository has a feature {string} with a strategy", async function(key: string, table: DataTable) {
  const world = this as SdkWorld;

  await waitForExpect(async () => {
    for (const row of table.hashes()) {
      const ctx = await world.context.attribute_values((row['fieldName'] as string), (row['values'] as string).split(',').map(t => t.trim())).build();
      expect(ctx.feature(key).isSet()).to.be.true;
      const value = ctx.feature(key).value.toString();
      const expectedValue = row['expectedValue'].toString();
      expect(value, `expected ${expectedValue} but got ${value}`).to.eq(expectedValue);
    }
  }, 5000, 300);
});
