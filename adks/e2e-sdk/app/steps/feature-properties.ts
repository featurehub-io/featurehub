import {Then, When} from "@cucumber/cucumber";
import {SdkWorld} from "../support/world";
import waitForExpect from "wait-for-expect";
import {FeatureStateHolder} from "featurehub-javascript-node-sdk";
import {logger} from "../support/logging";
import {expect} from "chai";
import {RoleType} from "../apis/mr-service";
import DataTable from "@cucumber/cucumber/lib/models/data_table";


When("there is no enriched data", async function() {
  const world = (this as SdkWorld);

  await waitForExpect(() => {
    // const f = this.featureState(this.feature.key) as FeatureStateHolder;
    const f = world.featureState(world.feature.key) as FeatureStateHolder;
    logger.debug(`enriched key is ${f.key}, enriched data is ${f.featureProperties}`);
    expect(f.featureProperties).to.be.undefined;
  }, 10000, 500);
});

When(/^we update the metadata to include '(.*)'$/, async function(metadata: string) {
  const world = (this as SdkWorld);
  const feature = (await world.featureApi.getFeatureByKey(world.application.id, world.feature.key, true)).data;
  feature.metaData = metadata;
  await world.featureApi.updateFeatureForApplication(world.application.id, feature.key, feature)
});

When('we allow the service account access to the enriched data', async function() {
  const world = (this as SdkWorld);

  const envId = world.environment.id;

  logger.info(`using service account ${world.serviceAccount}`);
  const sa = (await world.serviceAccountApi.getServiceAccount(world.serviceAccount.id, true)).data;
  const perm = sa.permissions.find(sap =>  sap.environmentId === envId );
  if (!perm.permissions.includes(RoleType.ExtendedData)) {
    perm.permissions.push(RoleType.ExtendedData);
    await world.serviceAccountApi.updateServiceAccount(sa.id, sa);
  }
});

Then('there is enriched data', async function(table: DataTable) {
  const world = (this as SdkWorld);
  const data: any = {};
  for(const row of table.hashes()) {
    let val = row['value']?.trim();
    let fieldName: string = row['field'];

    switch(val) {
      case 'portfolio.name':
        val = world.portfolio.name;
        break;
      case 'application.name':
        val = world.application.name;
        break;
    }

    data[fieldName] = val;
  }

  await waitForExpect(() => {
    const f = world.featureState(world.feature.key) as FeatureStateHolder;

    logger.info(`${f.key} feature properties are ${JSON.stringify(f.featureProperties)} and we want ${JSON.stringify(data)}`);
    logger.info(`full data ${JSON.stringify(f)}`);
    expect(f.featureProperties).to.deep.eq(data);
  }, 10000, 1000);
});
