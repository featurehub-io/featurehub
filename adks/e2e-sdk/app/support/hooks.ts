import { After, Before, BeforeAll } from '@cucumber/cucumber';
import { AuthServiceApi, PortfolioServiceApi, SetupServiceApi, UserCredentials } from '../apis/mr-service';
import { makeid } from './random';
import { SdkWorld } from './world';
import { discover } from './discovery';
import {resetCloudEvents, startWebServer, terminateServer} from './make_me_a_webserver';

const superuserEmailAddress = 'irina@i.com';
// const superuserEmailAddress = 'superuser@mailinator.com';
const superuserPassword = 'password123';

async function ensureLoggedIn(world: SdkWorld) {
  const portfolioService: PortfolioServiceApi = world.portfolioApi;

  try {
    const result = await portfolioService.findPortfolios();
  } catch (e: any) {
    if (e.response?.status == 401) {
      const loginApi: AuthServiceApi = world.loginApi;

      try {
        const loginResult = await loginApi.login(new UserCredentials({
          password: superuserPassword,
          email: superuserEmailAddress,
        }));

        console.log('logged in', loginResult.data);
        world.apiKey = loginResult.data;
        world.person = (await world.personApi.getPerson('self')).data;
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
          world.person = (await world.personApi.getPerson('self')).data;
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
  resetCloudEvents();
});

Before(function () {
  this.setScenarioId(makeid(30));
});

After(function () {
  const world = this as SdkWorld;
  if (world.edgeServer) {
    console.log('shutting down edge connection', world.edgeServer.getApiKeys(), world.edgeServer.url());
    world.edgeServer.close();
    console.log('edge connection closed');
  }
});

Before('@needs-webserver', async function() {
  await startWebServer();
});

After('@needs-webserver', async function() {
  await terminateServer();
});

BeforeAll(async function() {
  await discover();
});
