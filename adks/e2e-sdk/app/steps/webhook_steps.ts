import { Given, Then } from '@cucumber/cucumber';
import { SdkWorld } from '../support/world';
import { WebhookCheck } from 'featurehub-javascript-admin-sdk';
import waitForExpect from 'wait-for-expect';
import { getWebhookData } from '../support/make_me_a_webserver';
import { expect } from 'chai';

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
  await waitForExpect(async () => {
    const webhookData = getWebhookData();
    expect(webhookData).to.not.be.undefined;
    expect(webhookData.environment).to.not.be.undefined;
    expect(webhookData.environment.fv).to.not.be.undefined;
    const feature = webhookData.environment?.fv?.find(fv => fv.feature.key == flagName);
    expect(feature).to.not.be.undefined;
    expect(feature.value.locked).to.eq(lockedStatus === 'locked');
    expect(feature.value.value).to.eq(flag === 'on');
  }, 10000, 200);
});

Then(/^we should have 3 messages in the list of webhooks$/, async function() {
  if (!process.env.EXTERNAL_NGROK && process.env.REMOTE_BACKEND) {
    return;
  }
  const world = this as SdkWorld;
  const hookResults = (await world.webhookApi.listWebhooks(world.environment.id)).data;
  expect(hookResults.results.length).to.eq(3);
});
