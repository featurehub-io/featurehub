import { When } from '@cucumber/cucumber';
import { SdkWorld } from '../support/world';
import { expect } from 'chai';
import DataTable from '@cucumber/cucumber/lib/models/data_table';
import {
  FeatureGroup,
  FeatureGroupCreate,
  GroupRolloutStrategy,
  FeatureGroupUpdate,
  FeatureGroupUpdateFeature,
  RolloutStrategyAttribute,
  RolloutStrategyAttributeConditional,
  RolloutStrategyFieldType
} from '../apis/mr-service';

async function translateFeatureValueTable(world: SdkWorld, table: DataTable): Promise<Array<FeatureGroupUpdateFeature>> {
  const appFeatureResult = await world.featureApi.getAllFeaturesForApplication(world.application.id, false);

  expect(appFeatureResult.status).to.eq(200);

  const appFeatures = appFeatureResult.data;

  const features: Array<FeatureGroupUpdateFeature> = [];

  for (const row of table.hashes()) {
    const key = row['key'].toLowerCase();
    const featureId = appFeatures.find(f => f.key.toLowerCase() === key);
    expect(featureId,
      `Cannot find key ${key} in application feature keys ${appFeatures.map(s => s.key)}`)
      .to.not.be.undefined;
    features.push(new FeatureGroupUpdateFeature({
      id: featureId.id,
      value: row['value']
    }));
  }

  return features;
}

When("I create a feature group {string}", async function (groupName: string, table: DataTable) {
  const world = this as SdkWorld;

  expect(world.application).to.not.be.undefined;
  expect(world.environment).to.not.be.undefined;


  const fg = new FeatureGroupCreate({
    name: groupName,
    description: groupName,
    environmentId: world.environment.id,
    features: (await translateFeatureValueTable(world, table))
  });

  const data = await world.featureGroupApi.createFeatureGroup(world.application.id, fg);
  expect(data.status).to.eq(201);
  const g = data.data;
  world.featureGroup = new FeatureGroup({
    id: g.id,
    name: g.name,
    version: g.version,
    environmentId: g.environmentId,
    features: [],
    strategies: []
  });
});

export function conditional(cond: string): RolloutStrategyAttributeConditional {
  switch (cond.toLowerCase()) {
    case 'l':
      return RolloutStrategyAttributeConditional.Less;
    case 'le':
      return RolloutStrategyAttributeConditional.LessEquals;
    case 'g':
      return RolloutStrategyAttributeConditional.Greater;
    case 'ge':
      return RolloutStrategyAttributeConditional.GreaterEquals;
    case 'contains':
    case 'includes':
      return RolloutStrategyAttributeConditional.Includes;
    case 'excludes':
      return RolloutStrategyAttributeConditional.Excludes;
    case 'regex':
      return RolloutStrategyAttributeConditional.Regex;
    case 'sw':
      return RolloutStrategyAttributeConditional.StartsWith;
    case 'ew':
      return RolloutStrategyAttributeConditional.EndsWith;
    case 'ne':
      return RolloutStrategyAttributeConditional.NotEquals;
    default:
      return RolloutStrategyAttributeConditional.Equals;
  }
}

export function attributeType(type: string): RolloutStrategyFieldType {
  switch(type.toLowerCase()) {
    case 'sv':
    case 'semantic-version':
      return RolloutStrategyFieldType.SemanticVersion;
    case 'number':
      return RolloutStrategyFieldType.Number;
    case 'date':
      return RolloutStrategyFieldType.Date;
    case 'datetime':
      return RolloutStrategyFieldType.Datetime;
    case 'flag':
    case 'boolean':
      return RolloutStrategyFieldType.Boolean;
    case 'ip':
      return RolloutStrategyFieldType.IpAddress;
    case 'string':
    default:
      return RolloutStrategyFieldType.String;
  }
}

When('I delete the feature group {string}', async function(name: string) {
  const world = this as SdkWorld;

  expect(world.featureGroup).to.not.be.undefined;
  expect(world.featureGroup.name).to.eq(name);
  expect(world.application).to.not.be.undefined;
  expect(world.environment).to.not.be.undefined;

  await world.featureGroupApi.deleteFeatureGroup(world.application.id, world.featureGroup.id);
});

function extractStrategy(table: DataTable): GroupRolloutStrategy {
  const strategies: Array<GroupRolloutStrategy> = [];

  for (const row of table.hashes()) {
    if (!strategies.find(s => s.name === row['name'])) {
      strategies.push(new GroupRolloutStrategy({
        name: row["name"],
        percentage: row['percentage'].trim() === '-' ? null : parseInt(row['percentage'].trim()),
        attributes: [],
      }));
    }
  }
  // now go thru again and add the attributes
  for (const row of table.hashes()) {
    console.log('strategies ', strategies, 'vs row name', row['name']);
    const strat = strategies.find(s => s.name === row['name']);
    if (row['fieldName'].trim().length > 0) {
      strat.attributes.push(new RolloutStrategyAttribute({
        fieldName: row['fieldName'].trim(),
        conditional: conditional(row['conditional'].trim()),
        values: row['values'].trim().split(','),
        type: attributeType(row['type'].trim())
      }));
    }
  }

  expect(strategies.length, `A feature group can only have one strategy and you have ${strategies.length}`).to.eq(1);

  return strategies[0];
}

When("I update the strategy in the feature group", async function (table: DataTable) {
  const world = this as SdkWorld;

  await getGroupFromServer(world);

  const update = new FeatureGroupUpdate({
    version: world.featureGroup.version,
    strategies: [extractStrategy(table)]
  });

  const data = await world.featureGroupApi.updateFeatureGroup(world.application.id, update);
  expect(data.status).to.eq(200);
  world.featureGroup.version = data.data.version;
});

async function getGroupFromServer(world: SdkWorld) {
  expect(world.featureGroup).to.not.be.undefined;
  expect(world.application).to.not.be.undefined;
  expect(world.environment).to.not.be.undefined;

  const getData = await world.featureGroupApi.getFeatureGroup(world.application.id, world.featureGroup.id);
  expect(getData.status).to.eq(200);
  world.featureGroup = getData.data;
}

When('I update the values of the feature group to', async function (table: DataTable) {
  const world = this as SdkWorld;

  await getGroupFromServer(world);

  const update = new FeatureGroupUpdate({
    version: world.featureGroup.version,
    features: (await translateFeatureValueTable(world, table))
  });

  const data = await world.featureGroupApi.updateFeatureGroup(world.application.id, update);
  expect(data.status).to.eq(200);
  world.featureGroup.version = data.data.version;
});
