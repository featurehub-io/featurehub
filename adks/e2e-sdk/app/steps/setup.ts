import {expect} from 'chai';
import {Given, Then, When} from '@cucumber/cucumber';
import {
  Application, CreateApplication,
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
import {EdgeFeatureHubConfig, EdgeType, FeatureHubPollingClient, Readyness} from 'featurehub-javascript-node-sdk';
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
  await createApplication(this.portfolio.name, this as SdkWorld);
});

async function createApplication(name: string, world: SdkWorld) {
  // now create the app & environment
  const aCreate = await world.applicationApi.createApplication(world.portfolio.id, new CreateApplication({
    name: name,
    description: world.portfolio.name
  }), true, false);
  expect(aCreate.status).to.eq(200);
  // 1 environment, production
  expect(aCreate.data.environments.length).to.eq(1);
  world.application = aCreate.data;
  world.environment = aCreate.data.environments[0];
}

Given("I create an application {string}", async function(appName: string) {
  await createApplication(appName, this as SdkWorld);
});

Given("I restore the previous application", function() {
  const world = this as SdkWorld;
  expect(world.previousApplication).to.not.be.undefined;
  world.application = world.previousApplication;
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

Given("I create a service account called {string} with named permissions {string} with current environment", async function (saName: string, roles: string) {
  const world = this as SdkWorld;
  expect(world.application).to.not.be.undefined;
  expect(world.application.environments.length).to.not.eq(0);

  await serviceAccountPermissionRoles(world.application.environments[0].id, decodeAndValidateRoles(roles), world, saName);
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
  world.edgeServer?.close();
  await connectToFeatureServer(world);
})

Given(/^I connect to the feature server$/, async function () {
  const world = this as SdkWorld;
  await connectToFeatureServer(world);
});

Given('I connect to the feature server with poll {int}', async function (pollRate: number) {
  const world = this as SdkWorld;
  world.edgeServer?.close();
  await connectToFeatureServer(world, pollRate);
});

async function connectToFeatureServer(world: SdkWorld, forcePollRate = -1) {

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
    const fhConfig = new EdgeFeatureHubConfig(world.featureUrl, serviceAccountPerm.sdkUrlClientEval);
    if (forcePollRate !== -1) {
      fhConfig.restActive(forcePollRate);
    }

    world.sdkUrlClientEval = serviceAccountPerm.sdkUrlClientEval;
    world.sdkUrlServerEval = serviceAccountPerm.sdkUrlServerEval;

    // the node SDK is streaming by default, but env vars will automatically change it
    if (forcePollRate === -1 && !BackendDiscovery.supportsSSE && fhConfig.edgeType === EdgeType.STREAMING) {
      logger.info('Backend does not support SSE, using polling');
      fhConfig.restActive(200);
    }

    fhConfig.init();
    world.edgeServer = fhConfig;
    // give it time to connect
    await sleep(200);
    world.repository = fhConfig.repository();
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
