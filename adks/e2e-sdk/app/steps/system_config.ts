import {Given} from "@cucumber/cucumber";
import {getWebserverExternalAddress} from "../support/make_me_a_webserver";
import {SdkWorld} from "../support/world";
import {expect} from "chai";
import {UpdatedSystemConfig, UpdatedSystemConfigs} from "../apis/mr-service";


Given('I update the system config for Slack delivery', async function() {
  const slackApiUrl = `${getWebserverExternalAddress()}/featurehub/slack`;

  const fields = {
    'slack.enabled': true,
    'slack.bearerToken': '1234',
    'slack.defaultChannel': 'Cabcde',
    'slack.delivery.url': slackApiUrl,
  };

  const world = (this as SdkWorld);
  const sData = await world.systemConfigApi.getSystemConfig(['slack.']);
  expect(sData.status).to.eq(200);
  const configs = sData.data;

  const update = new UpdatedSystemConfigs({configs: []});
  for(let field in fields) {
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
});
