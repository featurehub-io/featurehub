import { After, AfterAll, Before, BeforeAll } from '@cucumber/cucumber';
import {
  Application,
  AuthServiceApi,
  Portfolio,
  PortfolioServiceApi,
  RoleType,
  ServiceAccount,
  ServiceAccountPermission,
  SetupServiceApi,
  UserCredentials
} from 'featurehub-javascript-admin-sdk';
import { makeid } from './random';
import { expect } from 'chai';
import { SdkWorld } from './world';
import { discover } from './discovery';
import { startWebServer, terminateServer } from './make_me_a_webserver';
import { lstat } from 'fs';

const superuserEmailAddress = 'irina@i.com';
// const superuserEmailAddress = 'superuser@mailinator.com';
const superuserPassword = 'password123';

async function ensureLoggedIn(world: SdkWorld) {
  const portfolioService: PortfolioServiceApi = world.portfolioApi;

  try {
    const result = await portfolioService.findPortfolios();
  } catch (e) {
    if (e.response?.status == 401) {
      const loginApi: AuthServiceApi = world.loginApi;

      try {
        const loginResult = await loginApi.login(new UserCredentials({
          password: superuserPassword,
          email: superuserEmailAddress,
        }));

        console.log('logged in', loginResult.data);
        world.apiKey = loginResult.data;
      } catch (loginError) {
        // console.log(loginError);
        const setupApi = new SetupServiceApi(world.adminApiConfig);
        try {
          const setupResult = await setupApi.setupSiteAdmin({
            portfolio: 'First Portfolio',
            organizationName: 'SampleOrg',
            emailAddress: superuserEmailAddress,
            password: superuserPassword,
            name: 'Superuser'
          });

          console.log('created account', setupResult.data);
          world.apiKey = setupResult.data;
        } catch (setupError) {
          console.error('Failed to create an account', setupError);
          process.exit(-1);
        }
      }
    }
  }
}

Before(async function () {
  await ensureLoggedIn(this as SdkWorld);
});

After(function () {
  if (this.edgeServer) {
    console.log('shutting down edge connection');
    this.edgeServer.close();
    console.log('edge connection closed');
  }
  terminateServer();
});

BeforeAll(async function() {
  startWebServer();
  await discover();
});
