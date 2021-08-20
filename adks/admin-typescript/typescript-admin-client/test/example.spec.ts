import { expect } from 'chai';
import { AuthServiceApi, Configuration, FeatureServiceApi, PortfolioServiceApi, UserCredentials } from '../app';

describe('Basic API testing', () => {
  it('We should be able to login and list features', async () => {
    const api = new Configuration({basePath: 'http://localhost:8085'});
    const login = await (new AuthServiceApi(api)).login(new UserCredentials({email: 'test@mailinator.com', password: 'password123'}));

    expect(login.status).to.eq(200);

    api.accessToken = login.data.accessToken;

    const portfolioApi = new PortfolioServiceApi(api);
    const portfolios = (await portfolioApi.findPortfolios(false, true)).data;
    expect(portfolios.length).to.eql(1);
    expect(portfolios[0].applications.length).to.eql(1);
    const featureApi = new FeatureServiceApi(api);
    const features = (await featureApi.findAllFeatureAndFeatureValuesForEnvironmentsByApplication(
      portfolios[0].applications[0].id)).data;
    expect(features.features.length).to.eql(5);
  });
});
