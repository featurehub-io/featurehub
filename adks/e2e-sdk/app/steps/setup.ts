import {expect} from 'chai';
import {Given, Then, When} from '@cucumber/cucumber';
import {
  Application,
  CreatePortfolio,
  Environment,
  PortfolioServiceApi,
  RoleType,
  ServiceAccount,
  ServiceAccountPermission,
  ServiceAccountServiceApi,
  UpdateEnvironment
} from '../apis/mr-service';
import {makeid, sleep} from '../support/random';
import {EdgeFeatureHubConfig, FeatureHubPollingClient, Readyness} from 'featurehub-javascript-node-sdk';
import waitForExpect from 'wait-for-expect';
import {logger} from '../support/logging';
import {SdkWorld} from '../support/world';
import {getWebserverExternalAddress} from '../support/make_me_a_webserver';
import {BackendDiscovery} from "../support/discovery";
import {decodeAndValidateRoles} from "../support/utils";
import {serviceAccountPermission, serviceAccountPermissionRoles} from "./service_accounts";

Given(/^I create a new portfolio$/, async function () {
  const world = this as SdkWorld;
  world.reset();
  const portfolioService: PortfolioServiceApi = world.portfolioApi;

  const name = process.env.PORTFOLIO_NAME || makeid(12);
  const pCreate = await portfolioService.createPortfolio(new CreatePortfolio({ name: name, description: name }));
  expect(pCreate.status).to.eq(200);
  this.portfolio = pCreate.data;
});

Given(/^I create an application$/, async function () {
  // now create the app & environment
  const aCreate = await this.applicationApi.createApplication(this.portfolio.id, new Application({
    name: this.portfolio.name,
    description: this.portfolio.name
  }), true, false);
  expect(aCreate.status).to.eq(200);
  // 1 environment, production
  expect(aCreate.data.environments.length).to.eq(1);
  this.application = aCreate.data;
  this.environment = aCreate.data.environments[0];
});

Given(/^I update the environment for feature webhooks$/, async function() {
  const world = this as SdkWorld;

  const app = await world.applicationApi.getApplication(world.application.id, true);
  world.application = app.data;
  world.environment = app.data.environments[0];

  // console.log("ensure previous environment has propagated");
  // await sleep(3);
  const env = world.environment;

  const webhookAddress = getWebserverExternalAddress();
  await world.environment2Api.updateEnvironmentV2(env.id, new UpdateEnvironment({
    version: env.version,
    webhookEnvironmentInfo: {
      'webhook.features.enabled': 'true',
      'webhook.features.endpoint': `${webhookAddress}/webhook`,
      'webhook.messages.enabled': 'false',
      'webhook.features.encrypt': 'webhook.features.endpoint,webhook.features.header.x-foof,webhook.features.header.authorisation',
      'webhook.features.header.x-foof': 'muddy',
      'webhook.features.header.authorisation': 'bearer 12345'
    }
  }));

  world.environment = (await world.applicationApi.getApplication(world.application.id, true)).data.environments[0];

  console.log('sleeping to ensure it is propagated');
  await sleep(3000);
});

Given(/^I create a new environment$/, async function () {
  // now create the app & environment
  const aCreate = await this.environmentApi.createEnvironment(this.application.id, new Environment({
    name: this.portfolio.name,
    description: this.portfolio.name
  }));
  expect(aCreate.status).to.eq(200);
  this.environment = aCreate.data;
});

Given(/^I delete the environment$/, async function () {
  // now create the app & environment
  const aCreate = await this.environmentApi.deleteEnvironment(this.environment.id, false, false);
  expect(aCreate.status).to.eq(200);
  expect(aCreate.data).to.be.true;
});


Given(/^I create a service account and (full|read) permissions based on the application environments$/, async function (roleTypes) {
  const world = this as SdkWorld;
  await serviceAccountPermission(world.application.environments[0].id, roleTypes, world);
});


Given("I create a service account with named permissions {string} with current environment", async function (roles: string) {
  const world = this as SdkWorld;
  expect(world.application).to.not.be.undefined;
  expect(world.application.environments.length).to.not.eq(0);

  await serviceAccountPermissionRoles(world.application.environments[0].id, decodeAndValidateRoles(roles), world)
});

Given('the edge connection is no longer available', async function () {
  const world = this as SdkWorld;

  await waitForExpect(async () => {
    const url = `${world.featureUrl}/features?apiKey=${this.serviceAccountPermission.sdkUrlClientEval}`;

    logger.info('Waiting for failed connection to feature server at `%s`', url);

    let found = false;
    try {
      await (FeatureHubPollingClient.pollingClientProvider({}, url, 0, (data) => {
        found = true;
        console.log('initial data is ', data);
      }).poll());
    } catch (ignored) {}

    expect(found).to.be.false;
  }, 10000, 1000);
});

When('I bounce the feature server connection', async function() {
  const world = this as SdkWorld;
  world.edgeServer.close();
  await connectToFeatureServer(world);
})

Given(/^I connect to the feature server$/, async function () {
  const world = this as SdkWorld;
  await connectToFeatureServer(world);
});

async function connectToFeatureServer(world: SdkWorld) {

  const serviceAccountPerm: ServiceAccountPermission = world.serviceAccountPermission;

  await waitForExpect(async () => {
    const url = `${world.featureUrl}/features?apiKey=${serviceAccountPerm.sdkUrlClientEval}`;

    logger.info('Waiting for successful connection to feature server at `%s`', url);

    let found = false;
    try {
      await (FeatureHubPollingClient.pollingClientProvider({}, url, 0, (data) => {
        found = true;
        logger.info('initial data is %s', JSON.stringify(data));
      }).poll());
    } catch (e) {
      logger.info('failed to poll %s', JSON.stringify(e));
    }

    expect(found, `${serviceAccountPerm.sdkUrlClientEval} failed to connect`).to.be.true;
    logger.info('Successfully completed poll');
    const edge = new EdgeFeatureHubConfig(world.featureUrl, serviceAccountPerm.sdkUrlClientEval);
    world.sdkUrlClientEval = serviceAccountPerm.sdkUrlClientEval;
    world.sdkUrlServerEval = serviceAccountPerm.sdkUrlServerEval;
    if (!BackendDiscovery.supportsSSE) {
      edge.edgeServiceProvider((repo, config) => new FeatureHubPollingClient(repo, config, 200));
    }

    edge.init();
    world.edgeServer = edge;
    // give it time to connect
    await sleep(200);
    world.repository = edge.repository();
    // its important we wait for it to become ready before stuffing data into it otherwise we can create features at the same
    // time we are waiting for a result back and miss the 1st feature
    await waitForExpect(() => {
      expect(world.repository).to.not.be.undefined;
      expect(world.repository.readyness).to.eq(Readyness.Ready);
      logger.info('repository is ready for action');
    }, 2000, 500);
  }, 4000, 200);
}

Then(/^I sleep for (\d+) seconds$/, async function (seconds) {
  await sleep(parseInt(seconds) * 1000);
});

Given('I am logged in and have a person configured', async function() {
  const world = this as SdkWorld;
  await world.getSelf();
  expect(world.person).to.not.be.undefined;
  expect(world.person.id).to.not.be.undefined;
  expect(world.person.id.id).to.not.be.undefined;
});
