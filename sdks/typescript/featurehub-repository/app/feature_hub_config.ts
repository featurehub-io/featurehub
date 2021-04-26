import { ClientFeatureRepository, FeatureHubRepository } from './client_feature_repository';
import { EdgeService } from './edge_service';
import { ClientContext, ClientEvalFeatureContext, ServerEvalFeatureContext } from './client_context';
import { FeatureHubPollingClient } from './polling_sdk';
import { InternalFeatureRepository } from './internal_feature_repository';

export type EdgeServiceProvider = (repository: InternalFeatureRepository, config: FeatureHubConfig) => EdgeService;
export type EdgeServiceSupplier = () => EdgeService;

export type FHLogMethod = (...args: any[]) => void;
export class FHLog {
  public log: FHLogMethod = (...args: any[]) => { console.log(args); };
  public error: FHLogMethod = (...args: any[]) => { console.error(args); };
}

export const fhLog = new FHLog();

export interface FeatureHubConfig {
  url(): string;

  // enable you to override the repository
  repository(repository?: FeatureHubRepository): FeatureHubRepository;

  // allow you to override the edge service provider
  edgeServiceProvider(edgeService?: EdgeServiceProvider): EdgeServiceProvider;

  // create a new context and allow you to pass in a repository and edge service
  newContext(repository?: FeatureHubRepository, edgeService?: EdgeServiceProvider): ClientContext;

  // is the repository client-side evaluated?
  clientEvaluated(): boolean;

  // add another API key
  apiKey(apiKey: string): FeatureHubConfig;

  // what are the API keys?
  getApiKeys(): Array<string>;

  // what is the host?
  getHost(): string;

  // initialize the connection outside of the creation of a context
  init(): FeatureHubConfig;

  // close any server connections
  close();
}

export class EdgeFeatureHubConfig implements FeatureHubConfig {
  private _host: string;
  private _apiKey: string;
  private _apiKeys: Array<string>;
  private _clientEval: boolean;
  private _url: string;
  private _repository: InternalFeatureRepository;
  private _edgeService: EdgeServiceProvider;
  private _edgeServices: Array<EdgeService> = [];

  static defaultEdgeServiceSupplier: EdgeServiceProvider = (repository, config) =>
    new FeatureHubPollingClient(repository, config, 6000);

  constructor(host: string, apiKey: string) {
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

    if (this._host.endsWith('/features/')) {
      this._host = this._host.substring(0, this._host.length - '/features/'.length);
    }

    this._url = this._host + 'features/' + this._apiKey;
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

  newContext(repository?: InternalFeatureRepository, edgeService?: EdgeServiceProvider): ClientContext {
    if (repository != null && edgeService != null) {
      return this._clientEval ?
        new ClientEvalFeatureContext(repository, this, edgeService(this._repository, this)) :
        new ServerEvalFeatureContext(repository, this, () => this._createEdgeService(this._edgeService));
    }

    this._repository = this._repository || new ClientFeatureRepository();
    this._edgeService = this._edgeService || EdgeFeatureHubConfig.defaultEdgeServiceSupplier;

    return this._clientEval ?
      new ClientEvalFeatureContext(this._repository, this, this._edgeService(this._repository, this)) :
      new ServerEvalFeatureContext(this._repository, this, () => this._createEdgeService(this._edgeService));
  }

  _createEdgeService(edgeServSupplier: EdgeServiceProvider) : EdgeService {
    const es = edgeServSupplier(this._repository, this);
    this._edgeServices.push(es);
    return es;
  }

  close() {
    this._edgeServices.forEach((es) => {
      es.close();
    })
  }

  init(): FeatureHubConfig {
    // ensure the repository exists
    this.repository();

    // ensure the edge service provider exists
    this._createEdgeService(this.edgeServiceProvider()).poll().catch((e) => fhLog.error(`Failed to connect to FeatureHub Edge ${e}`));

    return this;
  }

  edgeServiceProvider(edgeServ?: EdgeServiceProvider): EdgeServiceProvider {
    if (edgeServ != null) {
      this._edgeService = edgeServ;
    } else if (this._edgeService == null) {
      this._edgeService = EdgeFeatureHubConfig.defaultEdgeServiceSupplier;
    }

    return this._edgeService;
  }

  repository(repository?: InternalFeatureRepository): InternalFeatureRepository {
    if (repository != null) {
      this._repository = repository;
    } else if (this._repository == null) {
      this._repository = new ClientFeatureRepository();
    }

    return this._repository;
  }

  url(): string {
    return this._url;
  }
}
