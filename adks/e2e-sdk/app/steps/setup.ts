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
} from 'featurehub-javascript-admin-sdk';
import { makeid, sleep } from '../support/random';
import { EdgeFeatureHubConfig, FeatureHubPollingClient } from 'featurehub-javascript-node-sdk';
import waitForExpect from 'wait-for-expect';
import { logger } from '../support/logging';
import { SdkWorld } from '../support/world';

Given(/^I create a new portfolio$/, async function () {
  const world = this as SdkWorld;
  world.reset();
  const portfolioService: PortfolioServiceApi = world.portfolioApi;

  const name = makeid(12);
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

  const env = world.environment;

  await world.environment2Api.updateEnvironmentV2(env.id, new UpdateEnvironment({
    version: env.version,
    environmentInfo: {
      'webhook.features.enabled': 'true',
      'webhook.features.endpoint': 'http://localhost:4567',
      'webhook.features.headers': 'x-foof=muddy'
    }
  }));

  world.environment = (await world.applicationApi.getApplication(world.application.id, true)).data.environments[0];
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
  const roles = roleTypes === 'full' ? [RoleType.Read, RoleType.Unlock, RoleType.Lock, RoleType.ChangeValue] : [RoleType.Read];

  const permissions: ServiceAccountPermission[] = [
    new ServiceAccountPermission({
      environmentId: this.application.environments[0].id,
      permissions: roles
    })
  ];

  const serviceAccountApi: ServiceAccountServiceApi = this.serviceAccountApi;
  const serviceAccountCreate = await serviceAccountApi.createServiceAccountInPortfolio(this.portfolio.id, new ServiceAccount({
    name: this.portfolio.name, description: this.portfolio.name, permissions: permissions
  }), true);
  expect(serviceAccountCreate.status).to.eq(200);
  expect(serviceAccountCreate.data.permissions.length).to.eq(permissions.length);

  // this adds a new permission based on the environment we are actually in
  if (this.environment.id !== this.application.environments[0].id) {
    const updatedAccount = serviceAccountCreate.data;
    const newPerm = new ServiceAccountPermission({
      environmentId: this.environment.id,
      permissions: roles
    });

    updatedAccount.permissions.push(newPerm);
    permissions.push(newPerm);

    const saUpdate = await serviceAccountApi.updateServiceAccount(updatedAccount.id, updatedAccount, true);

    expect(saUpdate.status).to.eq(200);
    expect(saUpdate.data.permissions.length).to.eq(permissions.length);

    const accounts = await serviceAccountApi.searchServiceAccountsInPortfolio(this.portfolio.id, true,
      saUpdate.data.name, this.application.id, true);

    const sa = accounts.data.find(sa => sa.id == saUpdate.data.id);

    serviceAccountCreate.data.permissions = sa.permissions;
  }

  let perm: ServiceAccountPermission;

  for (const p of serviceAccountCreate.data.permissions) {
    if (p.environmentId === this.environment.id) {
      perm = p;
      break;
    }
  }

  expect(perm).to.not.be.undefined;

  this.serviceAccountPermission = perm;
  expect(perm.permissions.length).to.eq(roles.length);
  expect(perm.sdkUrlClientEval).to.not.be.undefined;
  expect(perm.sdkUrlServerEval).to.not.be.undefined;
  expect(perm.environmentId).to.not.be.undefined;
});

Given('the edge connection is no longer available', async function () {
  const world = this as SdkWorld;

  waitForExpect(async () => {
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
  });
});

Given(/^I connect to the feature server$/, function () {
  const serviceAccountPerm: ServiceAccountPermission = this.serviceAccountPermission;
  const world = this as SdkWorld;

  waitForExpect(async () => {
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
  }, 4000, 200);
});

Then(/^I sleep for (\d+) seconds$/, async function (seconds) {
  await sleep(parseInt(seconds) * 1000);
});
