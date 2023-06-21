import { Given, Then, When } from '@cucumber/cucumber';
import { SdkWorld } from '../support/world';
import { PersonType, WebhookCheck } from '../apis/mr-service';
import waitForExpect from 'wait-for-expect';
import { getWebhookData } from '../support/make_me_a_webserver';
import { expect } from 'chai';
import { sleep } from '../support/random';

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
