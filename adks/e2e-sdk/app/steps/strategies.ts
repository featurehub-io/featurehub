import { Then } from '@cucumber/cucumber';
import { SdkWorld } from '../support/world';
import DataTable from '@cucumber/cucumber/lib/models/data_table';
import { RolloutStrategy } from '../apis/mr-service';
import waitForExpect from 'wait-for-expect';
import { expect } from 'chai';

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
