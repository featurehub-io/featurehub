import { ClientFeatureRepository, FeatureHubRepository } from './client_feature_repository';
import { EdgeService } from './edge_service';
import { ClientContext, ClientEvalFeatureContext, ServerEvalFeatureContext } from './client_context';
import { FeatureHubPollingClient } from './polling_sdk';
import { InternalFeatureRepository } from './internal_feature_repository';

export type EdgeServiceSupplier = () => EdgeService;

export interface FeatureHubConfig {
  url(): string;
  repository(repository?: FeatureHubRepository): FeatureHubRepository;
  edgeService(edgeService?: EdgeServiceSupplier): EdgeServiceSupplier;
  newContext(repository?: FeatureHubRepository, edgeService?: EdgeServiceSupplier): ClientContext;
  clientEvaluated(): boolean;
  apiKey(apiKey: string): FeatureHubConfig;
  getApiKeys(): Array<string>;
  getHost(): string;
}

export class EdgeFeatureHubConfig implements FeatureHubConfig {
  private _host: string;
  private _apiKey: string;
  private _apiKeys: Array<string>;
  private _clientEval: boolean;
  private _url: string;
  private _repository: InternalFeatureRepository;
  private _edgeService: EdgeServiceSupplier;

  public EdgeFeatureHubConfig(host: string, apiKey: string) {
    this._apiKey = apiKey;
    this._host = host;

    if (apiKey == null || host == null) {
      throw new Error('apiKey and host must not be null');
    }

    this._apiKeys = [apiKey];

    this._clientEval = this._apiKey.includes('*');

    if (!this._host.endsWith('/')) {
      this._host += '/';
    }

    if (!this._host.endsWith('/features/')) {
      this._host += 'features/';
    }

    this._url = this._host + this._apiKey;
  }

  public apiKey(apiKey: string): FeatureHubConfig {
    this._apiKeys.push(apiKey);
    return this;
  }

  public clientEvaluated(): boolean {
    return this._apiKey.includes('*');
  }

  getApiKeys(): string[] {
    return Object.assign([], this._apiKeys);
  }

  getHost(): string {
    return this._host;
  }

  newContext(repository?: InternalFeatureRepository, edgeService?: EdgeServiceSupplier): ClientContext {
    if (repository != null && edgeService != null) {
      return this._clientEval ?
        new ClientEvalFeatureContext(repository, this, edgeService()) :
        new ServerEvalFeatureContext(repository, this, edgeService);
    }

    this._repository = this._repository || new ClientFeatureRepository();
    this._edgeService = this._edgeService || (() => new FeatureHubPollingClient(this._repository, this, 60));

    return this._clientEval ?
      new ClientEvalFeatureContext(this._repository, this, this._edgeService()) :
      new ServerEvalFeatureContext(this._repository, this, this._edgeService);
  }

  edgeService(edgeServ?: EdgeServiceSupplier): EdgeServiceSupplier {
    if (edgeServ != null) {
      this._edgeService = edgeServ;
    }

    return this._edgeService;
  }

  repository(repository?: InternalFeatureRepository): FeatureHubRepository {
    if (repository != null) {
      this._repository = repository;
    }

    return this._repository;
  }

  url(): string {
    return this._url;
  }
}
