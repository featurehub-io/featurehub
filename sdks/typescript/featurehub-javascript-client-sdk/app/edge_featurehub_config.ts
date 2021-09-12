import { InternalFeatureRepository } from './internal_feature_repository';
import { EdgeService } from './edge_service';
import { ClientFeatureRepository } from './client_feature_repository';
import { AnalyticsCollector } from './analytics';
import { FeatureStateValueInterceptor } from './interceptors';
import { ClientContext } from './client_context';
import { EdgeServiceProvider, FeatureHubConfig, fhLog } from './feature_hub_config';
import { Readyness, ReadynessListener } from './featurehub_repository';
import { ClientEvalFeatureContext, ServerEvalFeatureContext } from './context_impl';
import { FeatureHubEventSourceClient } from './featurehub_eventsource';

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
    new FeatureHubEventSourceClient(config, repository);

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
      this._host = this._host.substring(0, this._host.length - ('/features/'.length - 1));
    }

    this._url = this._host + 'features/' + this._apiKey;
  }

  public addReadynessListener(listener: ReadynessListener): void {
    this.repository().addReadynessListener(listener);
  }

  public addAnalyticCollector(collector: AnalyticsCollector): void {
    this.repository().addAnalyticCollector(collector);
  }

  public addValueInterceptor(interceptor: FeatureStateValueInterceptor): void {
    this.repository().addValueInterceptor(interceptor);
  }

  public get readyness(): Readyness {
    return this.repository().readyness;
  }

  public apiKey(apiKey: string): FeatureHubConfig {
    this._apiKeys.push(apiKey);
    return this;
  }

  public clientEvaluated(): boolean {
    return this._clientEval;
  }

  getApiKeys(): string[] {
    return Object.assign([], this._apiKeys);
  }

  getHost(): string {
    return this._host;
  }

  newContext(repository?: InternalFeatureRepository, edgeService?: EdgeServiceProvider): ClientContext {
    repository = repository || this.repository();
    edgeService = edgeService || this.edgeServiceProvider();

    return this._clientEval ?
      new ClientEvalFeatureContext(repository, this.getOrCreateEdgeService(edgeService, repository)) :
      new ServerEvalFeatureContext(repository, () => this.createEdgeService(edgeService, repository));
  }

  private getOrCreateEdgeService(edgeServSupplier: EdgeServiceProvider, repository?: InternalFeatureRepository): EdgeService {
    if (this._edgeServices.length === 0) {
      return this.createEdgeService(edgeServSupplier, repository);
    }

    return this._edgeServices[0];
  }

  private createEdgeService(edgeServSupplier: EdgeServiceProvider, repository?: InternalFeatureRepository): EdgeService {
    const es = edgeServSupplier(repository || this.repository(), this);
    this._edgeServices.push(es);
    return es;
  }

  close(): void {
    this._edgeServices.forEach((es) => {
      es.close();
    });
  }

  init(): FeatureHubConfig {
    // ensure the repository exists
    this.repository();

    // ensure the edge service provider exists
    this.createEdgeService(this.edgeServiceProvider()).poll().catch((e) => fhLog.error(`Failed to connect to FeatureHub Edge ${e}`));

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
