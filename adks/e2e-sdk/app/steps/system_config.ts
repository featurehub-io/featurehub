import {Given, Then} from "@cucumber/cucumber";
import {
  cloudEvents,
  getWebserverExternalAddress,
  resetSlackMessages,
  slackMessages
} from "../support/make_me_a_webserver";
import {SdkWorld} from "../support/world";
import {expect} from "chai";
import {UpdatedSystemConfig, UpdatedSystemConfigs} from "../apis/mr-service";
import waitForExpect from "wait-for-expect";


async function updateForSlack(world: SdkWorld, slackDeliveryUrl: string|undefined) {
  const fields = {
    'slack.enabled': true,
    'slack.bearerToken': '1234',
    'slack.defaultChannel': 'Cabcde',
    'slack.delivery.url': slackDeliveryUrl || '',
  };

  const sData = await world.systemConfigApi.getSystemConfig(['slack.']);
  expect(sData.status).to.eq(200);
  const configs = sData.data;

  const update = new UpdatedSystemConfigs({configs: []});
  for (let field in fields) {
    const existingVal = configs.configs.find(i => i.key == field);
    // @ts-ignore
    const v = fields[field];
    const val = new UpdatedSystemConfig({
      key: field,
      version: existingVal?.version ?? -1,
      value: v
    });
    update.configs.push(val);
  }

  await world.systemConfigApi.createOrUpdateSystemConfigs(update);

  const updatedConfigResult = await world.systemConfigApi.getSystemConfig(['slack.']);
  expect(updatedConfigResult.status).to.eq(200);
  const updatedCOnfig = updatedConfigResult.data;
  expect(updatedCOnfig.configs.find(s => s.key === 'slack.enabled')?.value).to.eq(true);
  expect(updatedCOnfig.configs.find(s => s.key === 'slack.delivery.url')?.value).to.eq(slackDeliveryUrl || '');

  const webhooks = await world.webhookApi.getWebhookTypes();
  expect(webhooks.status).to.eq(200);
  expect(webhooks.data.types.find(s => s.messageType === 'integration/slack-v1'), `Unable to find slack webhook in ${JSON.stringify(webhooks.data)}`).to.not.be.undefined;
}

Given('I update the system config for Slack delivery to external source', async function() {
  await updateForSlack(this as SdkWorld, `${getWebserverExternalAddress()}/featurehub/slack`);
});

Given('I update the system config for Slack delivery direct to Slack', async function() {
  await updateForSlack(this as SdkWorld, undefined);
});

Given('I redirect slack.com traffic to this testing service', async function() {
  const world = this as SdkWorld;

  const response = await fetch(`${world.adminApiConfig.basePath}/mr-api/testing/slack-testing`, {
    method: 'POST',
    headers: {
      'content-type': 'application/json'
    },
    body: JSON.stringify({'slack-url': `${getWebserverExternalAddress()}/slack.com/api`})
  });

  expect(response.status).to.eq(200);
});

Given('I clear slack events', () => {
  resetSlackMessages();
});

Then("I receive a slack message containing the key {string}", async function (key : string) {
  const world = this as SdkWorld;
  await waitForExpect(async () => {
    const found = slackMessages.find(m => JSON.stringify(m.body).includes(key));
    expect(found).to.not.be.undefined;
  }, 5000, 200);
});
