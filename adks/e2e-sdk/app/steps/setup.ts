import { expect } from 'chai';
import { Given, Then } from '@cucumber/cucumber';
import {
  Application,
  Environment,
  Portfolio,
  PortfolioServiceApi,
  RoleType,
  ServiceAccount,
  ServiceAccountPermission, ServiceAccountServiceApi, UpdateEnvironment
} from '../apis/mr-service';
import { makeid, sleep } from '../support/random';
import { EdgeFeatureHubConfig, FeatureHubPollingClient, Readyness } from 'featurehub-javascript-node-sdk';
import waitForExpect from 'wait-for-expect';
import { logger } from '../support/logging';
import { SdkWorld } from '../support/world';
import { getWebserverExternalAddress } from '../support/make_me_a_webserver';
import { timeout } from 'nats/lib/nats-base-client/util';

Given(/^I create a new portfolio$/, async function () {
  const world = this as SdkWorld;
  world.reset();
  const portfolioService: PortfolioServiceApi = world.portfolioApi;

  const name = process.env.PORTFOLIO_NAME || makeid(12);
  const pCreate = await portfolioService.createPortfolio(new Portfolio({ name: name, description: name }));
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
    environmentInfo: {
      'webhook.features.enabled': 'true',
      'webhook.features.endpoint': `${webhookAddress}/webhook`,
      'webhook.features.headers': 'x-foof=muddy,authorisation=bearer 12345'
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

async function serviceAccountPermission(envId: string, roleTypes: string, world: SdkWorld) {
  const roles = roleTypes === 'full' ? [RoleType.Read, RoleType.Unlock, RoleType.Lock, RoleType.ChangeValue] : [RoleType.Read];
  const permissions: ServiceAccountPermission[] = [
    new ServiceAccountPermission({
      environmentId: envId,
      permissions: roles
    })
  ];

  const serviceAccountApi: ServiceAccountServiceApi = world.serviceAccountApi;
  const serviceAccountCreate = await serviceAccountApi.createServiceAccountInPortfolio(world.portfolio.id, new ServiceAccount({
    name: world.portfolio.name, description: world.portfolio.name, permissions: permissions
  }), true);
  expect(serviceAccountCreate.status).to.eq(200);
  expect(serviceAccountCreate.data.permissions.length).to.eq(permissions.length);

  // this adds a new permission based on the environment we are actually in
  if (world.environment.id !== world.application.environments[0].id) {
    const updatedAccount = serviceAccountCreate.data;
    const newPerm = new ServiceAccountPermission({
      environmentId: world.environment.id,
      permissions: roles
    });

    updatedAccount.permissions.push(newPerm);
    permissions.push(newPerm);

    const saUpdate = await serviceAccountApi.updateServiceAccount(updatedAccount.id, updatedAccount, true);

    expect(saUpdate.status).to.eq(200);
    expect(saUpdate.data.permissions.length).to.eq(permissions.length);

    const accounts = await serviceAccountApi.searchServiceAccountsInPortfolio(world.portfolio.id, true,
      saUpdate.data.name, world.application.id, true);

    const sa = accounts.data.find(sa => sa.id == saUpdate.data.id);

    serviceAccountCreate.data.permissions = sa.permissions;
  }

  let perm: ServiceAccountPermission;

  for (const p of serviceAccountCreate.data.permissions) {
    if (p.environmentId === world.environment.id) {
      perm = p;
      break;
    }
  }

  expect(perm).to.not.be.undefined;

  world.serviceAccountPermission = perm;
  expect(perm.permissions.length).to.eq(roles.length);
  expect(perm.sdkUrlClientEval).to.not.be.undefined;
  expect(perm.sdkUrlServerEval).to.not.be.undefined;
  expect(perm.environmentId).to.not.be.undefined;
}

Given(/^I create a service account and (full|read) permissions for environment (.*)$/, async function (roleTypes: string, environment: string) {
  const world = this as SdkWorld;
  const env = world.application.environments.find(e => e.name === environment);
  expect(env, `Unable to find environment ${environment} in application`).to.not.be.undefined;

  await serviceAccountPermission(env.id, roleTypes, world);
});

Given(/^I create a service account and (full|read) permissions based on the application environments$/, async function (roleTypes) {
  const world = this as SdkWorld;
  await serviceAccountPermission(world.application.environments[0].id, roleTypes, world);
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

Given(/^I connect to the feature server$/, async function () {
  const world = this as SdkWorld;
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
    this.sdkUrlClientEval = serviceAccountPerm.sdkUrlClientEval;
    this.sdkUrlServerEval = serviceAccountPerm.sdkUrlServerEval;
    //edge.edgeServiceProvider((repo, config) => new FeatureHubPollingClient(repo, config, 200));
    edge.init();
    world.edgeServer = edge;
    // give it time to connect
    sleep(200);
    world.repository = edge.repository();
    // its important we wait for it to become ready before stuffing data into it otherwise we can create features at the same
    // time we are waiting for a result back and miss the 1st feature
    await waitForExpect(() => {
      expect(world.repository).to.not.be.undefined;
      expect(world.repository.readyness).to.eq(Readyness.Ready);
      logger.info('repository is ready for action');
    }, 2000, 500);
  }, 4000, 200);
});

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
