import {Given, Then, When} from "@cucumber/cucumber";
import {cloudEvents, getWebserverExternalAddress, resetCloudEvents} from "../support/make_me_a_webserver";
import waitForExpect from "wait-for-expect";
import {expect} from "chai";
import {SdkWorld} from "../support/world";
import {UpdateEnvironment} from "../apis/mr-service";
import {sleep} from "../support/random";
import {featurehubCloudEventBodyParser} from "featurehub-cloud-event-tools";

When('I clear cloud events', function() {
  resetCloudEvents();
});

Then('I receive a cloud event of type {string}', async function (ceType: string) {
  const world = this as SdkWorld;
  await waitForExpect(async () => {
    const events = cloudEvents.filter(ce => ce.type === ceType);
    expect(events.length).to.be.gt(0);
    const found = events.find((ce) => {
      const msg = featurehubCloudEventBodyParser<any>(ce);
      if (msg !== undefined) {
        return msg.eId === world.environment.id && msg.aId === world.application.id && world.portfolio.id === msg.pId;
      }
    });
    expect(found, `Events ${events} did not match environment ${world.environment.id}, appId ${world.application.id}, portfolio ${world.portfolio.id}`).to.not.be.undefined;
  }, 10000, 500);
});

Given(/^I update the environment for Slack$/, async function() {
  const world= this as SdkWorld;

  if (world.environment === undefined) {
    const app = await world.applicationApi.getApplication(world.application.id, true);
    world.application = app.data;
    world.environment = app.data.environments[0];
  }

  // console.log("ensure previous environment has propagated");
  // await sleep(3);
  const env = world.environment;

  const webhookAddress = getWebserverExternalAddress();
  await world.environment2Api.updateEnvironmentV2(env.id, new UpdateEnvironment({
    version: env.version,
    webhookEnvironmentInfo: {
      'integration.slack.enabled': 'true',
      'integration.slack.encrypt': 'integration.slack.token',
      'integration.slack.token': 'abcd1234%^&',
      'integration.slack.channel_name': '@Uabcdefg',
    }
  }));

  world.environment = (await world.applicationApi.getApplication(world.application.id, true)).data.environments[0];

  console.log('sleeping to ensure it is propagated');
  await sleep(3000);
});
