import { expect } from 'chai';
import { Given } from '@cucumber/cucumber';
import {
  Application,
  Portfolio,
  PortfolioServiceApi,
  RoleType,
  ServiceAccount,
  ServiceAccountPermission
} from 'featurehub-javascript-admin-sdk';
import { makeid, sleep } from '../support/random';
import { EdgeFeatureHubConfig, FeatureHubPollingClient } from 'featurehub-javascript-node-sdk';
import waitForExpect from 'wait-for-expect';
import { logger } from '../support/logging';
import { SdkWorld } from '../support/world';

Given(/^I create a new portfolio$/, async function () {
  const portfolioService: PortfolioServiceApi = this.portfolioApi;

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
});

Given(/^I create a service account and full permissions based on the application environments$/, async function () {
  const serviceAccountCreate = await this.serviceAccountApi.createServiceAccountInPortfolio(this.portfolio.id, new ServiceAccount({
    name: this.portfolio.name, description: this.portfolio.name, permissions:
      [ new ServiceAccountPermission({
        environmentId: this.application.environments[0].id,
        permissions: [ RoleType.Read, RoleType.Unlock, RoleType.Lock, RoleType.ChangeValue ]
      }) ]
  }), true);
  expect(serviceAccountCreate.status).to.eq(200);
  expect(serviceAccountCreate.data.permissions.length).to.eq(1);
  const perm = serviceAccountCreate.data.permissions[0];
  this.serviceAccountPermission = perm;
  expect(perm.permissions.length).to.eq(4);
  expect(perm.sdkUrlClientEval).to.not.be.undefined;
  expect(perm.sdkUrlServerEval).to.not.be.undefined;
  expect(perm.environmentId).to.not.be.undefined;
});

Given(/^I connect to the feature server$/, function () {
  const serviceAccountPerm: ServiceAccountPermission = this.serviceAccountPermission;
  const world = this;

  waitForExpect(async () => {
    const url = `${world.featureUrl}/features?apiKey=${serviceAccountPerm.sdkUrlClientEval}`;

    logger.info('Waiting for successful connection to feature server at `%s`', url);

    let found = false;
    await (FeatureHubPollingClient.pollingClientProvider({}, url, 0, (data) => {
      found = true;
      console.log('initial data is ', data);
    }).poll());

    expect(found).to.be.true;
    logger.info('Successfully completed poll');
    const edge = new EdgeFeatureHubConfig(world.featureUrl, serviceAccountPerm.sdkUrlClientEval);
    edge.init();
    world.edgeServer = edge;
    // give it time to connect
    sleep(200);
    world.repository = edge.repository();
  }, 4000, 200);
});
