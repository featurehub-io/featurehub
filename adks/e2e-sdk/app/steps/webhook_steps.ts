import {Given, Then, When} from '@cucumber/cucumber';
import {SdkWorld} from '../support/world';
import {CreateFeature, FeatureValue, FeatureValueType, Person, PersonType, WebhookCheck} from '../apis/mr-service';
import waitForExpect from 'wait-for-expect';
import {cloudEvents, resetCloudEvents} from '../support/make_me_a_webserver';
import {expect} from 'chai';
import {sleep} from '../support/random';
import DataTable from '@cucumber/cucumber/lib/models/data_table';
import {EnrichedFeatures} from '../apis/webhooks';
import {logger} from '../support/logging';
import {CloudEvent} from "cloudevents";
import {featurehubCloudEventBodyParser} from "featurehub-cloud-event-tools";

When('I wait for {int} seconds', async function (seconds: number) {
  await sleep(seconds * 1000);
});

Given(/^I test the webhook$/, async function () {
  const world = this as SdkWorld;

  const webhookType = (await world.webhookApi.getWebhookTypes()).data.types[0];
  if (!process.env.EXTERNAL_NGROK && process.env.REMOTE_BACKEND) {
    return;
  }

  await world.webhookApi.testWebhook(new WebhookCheck({
    messageType: webhookType.messageType,
    envId: world.environment.id,
    config: {}
  }));
});

Given('I clear the cloud events', function () {
  resetCloudEvents();
});

function ourEnrichedFeatures(world: SdkWorld): CloudEvent<EnrichedFeatures>[] {
  return cloudEvents.filter(ce => ce.type == 'enriched-feature-v1'
    && (ce.data as EnrichedFeatures)?.environment?.environment?.id === world.environment.id)
    .map(ce => ce as CloudEvent<EnrichedFeatures>);
}

Then(/^we receive a webhook with (.*) flag that is (locked|unlocked) and (off|on) and version (.*)$/, async function(flagName: string,
                                                                                                                     lockedStatus: string, flag: string, version: string) {
  if (!process.env.EXTERNAL_NGROK && process.env.REMOTE_BACKEND) {
    return;
  }

  const world = this as SdkWorld;

  console.log('looking for ', flagName, lockedStatus, flag, version);
  await waitForExpect(async () => {
    const enrichedData = ourEnrichedFeatures(world);

    expect(enrichedData.length, `filtered events for enriched and for our environment and found none ${cloudEvents}`).to.be.gt(0);

    const ourFeature = enrichedData.filter(ce => {
      const featureData = featurehubCloudEventBodyParser(ce) as EnrichedFeatures;
      console.log('feature data is ', featureData, featureData?.environment?.fv);
      const feature = featureData.environment?.fv?.find(fv => fv.feature.key === flagName);
      return (feature != null && feature.value != null && feature.value.locked === (lockedStatus === 'locked') &&
              feature.value.value === (flag === 'on') &&
              feature.value.version == parseInt(version) &&
              feature.value.pId === world.person.id.id);
    });

    expect(ourFeature, `could not find feature ${flagName} in status ${lockedStatus} with value ${flag} and person ${world.person.id.id} in ${enrichedData}`)
      .to.not.be.undefined;
    expect(ourFeature, `could not find feature ${flagName} in status ${lockedStatus} with value ${flag} and person ${world.person.id.id} in ${enrichedData}`)
      .to.not.be.empty;
  }, 10000, 200);
});

Then(/^we should have (\d+) messages in the list of webhooks$/, async function(resultCount: number) {
  if (!process.env.EXTERNAL_NGROK && process.env.REMOTE_BACKEND) {
    return;
  }
  const world = this as SdkWorld;
  await waitForExpect(async () => {
    const enrichedData = ourEnrichedFeatures(world);
    expect(enrichedData.length, `filtered events for enriched and found wrong number ${cloudEvents}`).to.be.gt(resultCount - 1);
    expect(enrichedData.length, `filtered events for enriched and found wrong number ${cloudEvents}`).to.be.lt(resultCount + 2);
  }, 2000, 200);
});

Then(/^we receive a webhook that has changed the feature (.*) that belongs to the Test SDK$/, async function (key: string) {
  const world = this as SdkWorld;
  await waitForExpect(async () => {
    const enrichedData = ourEnrichedFeatures(world);
    const ourFeature = enrichedData.filter(ce => {
      const featureData = ce.data as EnrichedFeatures;
      const feature = featureData.environment?.fv?.find(fv => fv.feature.key === key);
      return feature.value.pId !== undefined;
    });

    expect(ourFeature.length, `Could not find person associated with enriched data ${enrichedData}`).to.be.gt(0);
    const features = ourFeature.map(ed =>
      (ed.data as EnrichedFeatures).environment?.fv?.find(fv => fv.feature.key === key) )
      .filter( feature => feature?.value.pId )


    let actionByServiceAccountCount = 0;
    let users: Array<Person> = [];
    for(let feature of features) {
      const user = await world.personApi.getPerson(feature.value.pId);

      users.push(user.data);

      if (user.data.personType === PersonType.SdkServiceAccount && user.data.additional.find(k => k.key === 'serviceAccountId')) {
        actionByServiceAccountCount ++;
      }
    }

    expect(actionByServiceAccountCount, `None of the users are service account users who performed the action: ${users}`).to.be.gt(0);
  }, 10000, 200);
});

function featureType(name: string): FeatureValueType {
  if (!name?.trim()) return FeatureValueType.Boolean;
  if (name === 'flag') return FeatureValueType.Boolean;
  if (name === 'number') return FeatureValueType.Number;
  if (name === 'json') return FeatureValueType.Json;
  return FeatureValueType.String;
}

function featureValue(version: number, type: FeatureValueType, value: string, key: string): FeatureValue {
  const val = new FeatureValue({version: version, locked: false, key: key});

  switch (type) {
    case FeatureValueType.Boolean:
      val.valueBoolean = (value === 'true') || (value === 'on');
      break;
    case FeatureValueType.String:
      val.valueString = value;
      break;
    case FeatureValueType.Number:
      val.valueNumber = parseFloat(value);
      break;
    case FeatureValueType.Json:
      val.valueJson = value;
      break;
  }

  return val;
}

function expectedFeatures(webhookData: EnrichedFeatures | undefined, expectedFeatureKeys: Array<string>): boolean {
  if (webhookData === undefined) return false;

  logger.info(`Expecting keys ${expectedFeatureKeys} got ${webhookData.environment?.fv?.map(i => i.feature.key)}`);
  expectedFeatureKeys.forEach(key => {
    const feature = webhookData.environment?.fv?.find(fv => fv.feature.key == key);
    if (feature === undefined) {
      return false;
    }
  });

  return true;
}

async function createFeatureAndValue(world: SdkWorld, type: FeatureValueType,
                                     key: string, value: string, action: string, expectedFeatureKeys: Array<string>): Promise<void> {
  logger.info('createFeatureAndValue has started');
  await world.featureApi.createFeaturesForApplication(world.application.id, new CreateFeature({
    valueType: type,
    name: key,
    key: key,
    description: key
  }));

  // wait until the webhook turns up that creates the key
  await waitForExpect(async () => {
    const enrichedData = ourEnrichedFeatures(world);
    const keys = enrichedData.map(ef =>
      (ef.data as EnrichedFeatures).environment?.fv?.map(fv => fv.feature.key));

    expect(enrichedData.length).to.be.gt(0);
    for(let ce of enrichedData) {
      const ed = (ce.data as EnrichedFeatures);
      const keys = ed.environment?.fv?.map(fv => fv.feature.key);
      expect(keys, `${keys} from the cloud event ${JSON.stringify(ed)} were not the expected keys ${expectedFeatureKeys}`).to.have.members(expectedFeatureKeys);
      expect(ed.featureKeys, `${ed.featureKeys} keys passed did not include ${key}`).to.include(key);
    }
  }, 10000, 200);

  resetCloudEvents();

  if (action !== 'justcreate') {
    const version = type == FeatureValueType.Boolean ? 1 : 0;
    const fv = featureValue(version, type, value, key);
    await world.featureValueApi.updateFeatureForEnvironment(world.environment.id, key, fv);

    await waitForExpect(async () => {
      const enrichedData = ourEnrichedFeatures(world);
      expect(enrichedData.length).to.be.gt(0);
      for(let ce of enrichedData) {
        const ed = (ce.data as EnrichedFeatures);
        const feature = ed.environment?.fv?.find(fv => fv.feature.key == key);
        expect(feature?.value?.locked).to.be.false;
        const keyChange = ed.featureKeys.includes(key);
        expect(keyChange).to.be.true;
      }
    }, 10000, 200);

    resetCloudEvents();
  }

  logger.info('createFeatureAndValue has finished');
}

async function deleteFeature(world: SdkWorld, key: string, expectedFeatureKeys: Array<string>): Promise<void> {
  await world.featureApi.deleteFeatureForApplication(world.application.id, key);

  // wait until the webhook turns up that creates the key
  await waitForExpect(async () => {
    const enrichedData = ourEnrichedFeatures(world);
    expect(enrichedData.length).to.be.gt(0);
    for(let ce of enrichedData) {
      const ed = (ce.data as EnrichedFeatures);
      const keyChange = ed.featureKeys.includes(key);
      expect(keyChange).to.be.true;
    }
  }, 10000, 200);

  resetCloudEvents();
}

When(/^then I test the webhook$/, { timeout: 300000 }, async function(table: DataTable) {
  const world = this as SdkWorld;
  for(const row of table.hashes()) {
    console.log('processing row', row);
    resetCloudEvents();
    const type = featureType(row['feature_type']);
    if (row['action'] === 'create' || row['action'] === 'justcreate') {
      await createFeatureAndValue(world, type, row['key'], row['value'], row['action'], row['expected_features'].trim().split(','));
    } else if (row['action'] === 'delete') {
      await deleteFeature(world, row['key'], row['expected_features'].trim().split(','));
    } else if (row['action'] === 'change') {
      // await updateFeatureValue(world, type, row['key'], row['value']);
    }
  }
});
