import { After, AfterAll, Before } from '@cucumber/cucumber';
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

async function ensureLoggedIn(world: SdkWorld) {
  const portfolioService: PortfolioServiceApi = world.portfolioApi;

  try {
    const result = await portfolioService.findPortfolios();
  } catch (e) {
    if (e.response?.status == 401) {
      const loginApi: AuthServiceApi = world.loginApi;

      try {
        const loginResult = await loginApi.login(new UserCredentials({
          email: 'superuser@mailinator.com',
          password: 'password123'
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
            emailAddress: 'superuser@mailinator.com',
            password: 'password123',
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
    this.edgeServer.close();
  }
});
