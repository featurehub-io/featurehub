import { setDefaultTimeout, setWorldConstructor, World } from '@cucumber/cucumber';
import {
  Application,
  ApplicationServiceApi,
  AuthServiceApi,
  Configuration, Environment,
  EnvironmentFeatureServiceApi,
  EnvironmentServiceApi,
  Feature,
  FeatureServiceApi,
  FeatureValue,
  Portfolio,
  PortfolioServiceApi,
  ServiceAccountPermission,
  ServiceAccountServiceApi,
  TokenizedPerson
} from 'featurehub-javascript-admin-sdk';
import { axiosLoggingAttachment, logger } from './logging';
import globalAxios from 'axios';
import {
  ClientContext,
  EdgeFeatureHubConfig,
  FeatureHubRepository,
  FeatureStateHolder
} from 'featurehub-javascript-node-sdk';
import { expect } from 'chai';
import { edgeHost, mrHost } from './discovery';

let apiKey: string;

// axiosLoggingAttachment([ globalAxios ]);

export class SdkWorld extends World {
  private _portfolio: Portfolio;
  private _application: Application;
  public feature: Feature;
  public environment: Environment;
  public serviceAccountPermission: ServiceAccountPermission;
  public edgeServer: EdgeFeatureHubConfig;
  private _repository: FeatureHubRepository;
  public readonly adminUrl: string;
  public readonly featureUrl: string;
  public readonly adminApiConfig: Configuration;
  public readonly portfolioApi: PortfolioServiceApi;
  public readonly applicationApi: ApplicationServiceApi;
  public readonly environmentApi: EnvironmentServiceApi;
  public readonly featureApi: FeatureServiceApi;
  public readonly loginApi: AuthServiceApi;
  public readonly serviceAccountApi: ServiceAccountServiceApi;
  public readonly featureValueApi: EnvironmentFeatureServiceApi;
  private _clientContext: ClientContext;
  public sdkUrlClientEval: string;
  public sdkUrlServerEval: string;


  constructor(props) {
    super(props);

    this.adminUrl = mrHost();
    this.featureUrl = edgeHost();

    this.adminApiConfig = new Configuration({ basePath: this.adminUrl, apiKey: apiKey, axiosInstance: globalAxios.create() });
    this.portfolioApi = new PortfolioServiceApi(this.adminApiConfig);
    this.applicationApi = new ApplicationServiceApi(this.adminApiConfig);
    this.environmentApi = new EnvironmentServiceApi(this.adminApiConfig);
    this.featureApi = new FeatureServiceApi(this.adminApiConfig);
    this.loginApi = new AuthServiceApi(this.adminApiConfig); // too noisy in logs
    this.serviceAccountApi = new ServiceAccountServiceApi(this.adminApiConfig);
    this.featureValueApi = new EnvironmentFeatureServiceApi(this.adminApiConfig);

    axiosLoggingAttachment([this.adminApiConfig.axiosInstance]);
  }

  public reset(): void {
    this._application = undefined;
    this._portfolio = undefined;
    this.feature = undefined;
    this.environment = undefined;
    this.serviceAccountPermission = undefined;
    this.edgeServer = undefined;
  }

  public set repository(r: FeatureHubRepository) {
    this._repository = r;
  }

  public get repository() {
    return this._repository;
  }

  public featureState(key: string): FeatureStateHolder {
    if (this._clientContext) {
      return this._clientContext.feature(key);
    } else {
      return this.repository.feature(key);
    }
  }

  public get context(): ClientContext {
    if (!this._clientContext) {
      this._clientContext = this.edgeServer.newContext();
    }

    return this._clientContext;
  }

  public resetContext() {
    this._clientContext = this.edgeServer.newContext();
  }

  public set portfolio(p: Portfolio) {
    this._portfolio = p;
  }

  public get portfolio() {
    return this._portfolio;
  }

  public set application(a: Application) {
    this._application = a;
  }

  public get application() {
    return this._application;
  }

  public set apiKey(val: TokenizedPerson) {
    this.adminApiConfig.accessToken = val.accessToken;
    apiKey = val.accessToken;
    logger.info('Successfully logged in');
  }

  async getFeature(): Promise<FeatureValue> {
    try {
      const fValueResult = await this.featureValueApi.getFeatureForEnvironment(this.serviceAccountPermission.environmentId, this.feature.key);
      return fValueResult.data;
    } catch (e) {
      expect(e.response.status).to.eq(404); // null value

      if (e.response.status === 404) {
        return new FeatureValue({key: this.feature.key});
      }
    }
  }

  async updateFeature(fValue: FeatureValue) {
    fValue.whenUpdated = undefined;
    fValue.whoUpdated = undefined;
    const uResult = await this.featureValueApi.updateFeatureForEnvironment(this.serviceAccountPermission.environmentId, this.feature.key, fValue);
    expect(uResult.status).to.eq(200);
  }
}

setDefaultTimeout(30 * 1000);
setWorldConstructor(SdkWorld);
