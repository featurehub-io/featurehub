import { Given, Then, When } from '@cucumber/cucumber';
import { SdkWorld } from '../support/world';
import { Feature, FeatureValue, FeatureValueType, PersonType, WebhookCheck } from '../apis/mr-service';
import waitForExpect from 'wait-for-expect';
import { clearWebhookData, getWebhookData } from '../support/make_me_a_webserver';
import { expect } from 'chai';
import { sleep } from '../support/random';
import DataTable from '@cucumber/cucumber/lib/models/data_table';

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
  }));
});

Then(/^we receive a webhook with (.*) flag that is (locked|unlocked) and (off|on)$/, async function(flagName: string, lockedStatus: string, flag: string) {
  if (!process.env.EXTERNAL_NGROK && process.env.REMOTE_BACKEND) {
    return;
  }

  const world = this as SdkWorld;

  await waitForExpect(async () => {
    const webhookData = getWebhookData();
    expect(webhookData).to.not.be.undefined;
    expect(webhookData.environment).to.not.be.undefined;
    expect(webhookData.environment.featureValues).to.not.be.undefined;
    const feature = webhookData.environment?.featureValues?.find(fv => fv.feature.key == flagName);
    expect(feature).to.not.be.undefined;
    expect(feature.value.locked).to.eq(lockedStatus === 'locked');
    expect(feature.value.value).to.eq(flag === 'on');
    expect(feature.value.personIdWhoChanged).to.not.be.undefined;
    expect(feature.value.personIdWhoChanged).to.eq(world.person.id.id)
  }, 10000, 200);
});

Then(/^we should have (\d+) messages in the list of webhooks$/, async function(resultCount: number) {
  if (!process.env.EXTERNAL_NGROK && process.env.REMOTE_BACKEND) {
    return;
  }
  const world = this as SdkWorld;
  await waitForExpect(async () => {

    const hookResults = (await world.webhookApi.listWebhooks(world.environment.id)).data;
    expect(hookResults.results.length).to.eq(3);
  }, 2000, 200);
});

Then(/^we receive a webhook that has changed the feature (.*) that belongs to the Test SDK$/, async function (key: string) {
  const world = this as SdkWorld;
  await waitForExpect(async () => {
    const webhookData = getWebhookData();
    expect(webhookData).to.not.be.undefined;
    const feature = webhookData.environment?.featureValues?.find(fv => fv.feature.key == key);
    expect(feature.value.personIdWhoChanged).to.not.be.undefined;
    const user = await world.personApi.getPerson(feature.value.personIdWhoChanged);
    expect(user.data.personType).to.eq(PersonType.SdkServiceAccount);
    expect(user.data.additional.find(k => k.key === 'serviceAccountId')).to.not.be.undefined;
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

async function createFeatureAndValue(world: SdkWorld, type: FeatureValueType, key: string, value: string, action: string): Promise<void> {
  await world.featureApi.createFeaturesForApplication(world.application.id, new Feature({
    valueType: type,
    name: key,
    key: key,
    description: key
  }));

  // wait until the webhook turns up that creates the key
  await waitForExpect(async () => {
    const webhookData = getWebhookData();
    expect(webhookData).to.not.be.undefined;
    const feature = webhookData.environment?.featureValues?.find(fv => fv.feature.key == key);
    expect(feature).to.not.be.undefined;
    const keyChange = webhookData.featureKeys.includes(key);
    expect(keyChange).to.be.true;
  }, 10000, 200);

  clearWebhookData();

  if (action !== 'justcreate') {
    const version = type == FeatureValueType.Boolean ? 1 : 0;
    const fv = featureValue(version, type, value, key);
    await world.featureValueApi.updateFeatureForEnvironment(world.environment.id, key, fv);

    await waitForExpect(async () => {
      const webhookData = getWebhookData();
      expect(webhookData).to.not.be.undefined;
      const feature = webhookData.environment?.featureValues?.find(fv => fv.feature.key == key);
      expect(feature).to.not.be.undefined;
      expect(feature.value.locked).to.be.false;
      const keyChange = webhookData.featureKeys.includes(key);
      expect(keyChange).to.be.true;
    }, 10000, 200);

    clearWebhookData();
  }
}

async function deleteFeature(world: SdkWorld, key: string): Promise<void> {
  await world.featureApi.deleteFeatureForApplication(world.application.id, key);

  // wait until the webhook turns up that creates the key
  await waitForExpect(async () => {
    const webhookData = getWebhookData();
    expect(webhookData).to.not.be.undefined;
    const feature = webhookData.environment?.featureValues?.find(fv => fv.feature.key == key);
    expect(feature).to.be.undefined;
    const keyChange = webhookData.featureKeys.includes(key);
    expect(keyChange).to.be.true;
  }, 10000, 200);

  clearWebhookData();
}

When(/^then I test the webhook$/, { timeout: 300000 }, async function(table: DataTable) {
  const world = this as SdkWorld;
  for(const row of table.hashes()) {
    console.log('processing row', row);
    const type = featureType(row['feature_type']);
    if (row['action'] === 'create' || row['action'] === 'justcreate') {
      await createFeatureAndValue(world, type, row['key'], row['value'], row['action']);
    } else if (row['action'] === 'delete') {
      await deleteFeature(world, row['key']);
    } else if (row['action'] === 'change') {
      // await updateFeatureValue(world, type, row['key'], row['value']);
    }
  }
});
