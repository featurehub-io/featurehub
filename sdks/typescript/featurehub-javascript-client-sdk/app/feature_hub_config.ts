import { EdgeService } from './edge_service';
import { ClientContext } from './client_context';
import { InternalFeatureRepository } from './internal_feature_repository';
import { AnalyticsCollector } from './analytics';
import { FeatureStateValueInterceptor } from './interceptors';
import { FeatureHubRepository, Readyness, ReadynessListener } from './featurehub_repository';

export type EdgeServiceProvider = (repository: InternalFeatureRepository, config: FeatureHubConfig) => EdgeService;
export type EdgeServiceSupplier = () => EdgeService;

export type FHLogMethod = (...args: any[]) => void;

export class FHLog {
  public static fhLog = new FHLog();

  public log: FHLogMethod = (...args: any[]) => {
    console.log(args);
  }

  public error: FHLogMethod = (...args: any[]) => {
    console.error(args);
  }

  // tslint:disable-next-line:no-empty
  public trace: FHLogMethod = (...args: any[]) => {};

  public quiet() {
    // tslint:disable-next-line:no-empty
    FHLog.fhLog.log = (...args: any[]) => {
    };
    // tslint:disable-next-line:no-empty
    FHLog.fhLog.error = (...args: any[]) => {
    };

    // tslint:disable-next-line:no-empty
    FHLog.fhLog.trace = (...args: any[]) => {
    };
  }
}

export const fhLog = FHLog.fhLog;

export interface FeatureHubConfig {
  readyness: Readyness;

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

  // add a callback for when the system is ready
  addReadynessListener(listener: ReadynessListener): void;

  // add an analytics collector
  addAnalyticCollector(collector: AnalyticsCollector): void;

  // add a value interceptor (e.g. baggage handler)
  addValueInterceptor(interceptor: FeatureStateValueInterceptor): void;
}
