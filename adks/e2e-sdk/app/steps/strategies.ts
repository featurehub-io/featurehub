import { Then } from '@cucumber/cucumber';
import { SdkWorld } from '../support/world';
import DataTable from '@cucumber/cucumber/lib/models/data_table';
import { RolloutStrategy } from '../apis/mr-service';

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
