import { EdgeService, FeatureHubConfig, fhLog, SSEResultState } from 'featurehub-repository';
import EventSource from 'eventsource';
import {InternalFeatureRepository} from 'featurehub-repository/dist/internal_feature_repository';

export class FeatureHubEventSourceClient implements EdgeService {
  private eventSource: EventSource;
  private readonly _config: FeatureHubConfig;
  private readonly _repository: InternalFeatureRepository;
  private _header: string;

  constructor(config: FeatureHubConfig, repository: InternalFeatureRepository) {
    this._config = config;
    this._repository = repository;
  }

  init() {
    const options: any = {};
    if (this._header) {
      options.headers = {
        'x-featurehub': this._header
      };
    }
    fhLog.log('listening at ', this._config.url());
    this.eventSource = new EventSource(this._config.url(), options);

    [SSEResultState.Features, SSEResultState.Feature, SSEResultState.DeleteFeature,
        SSEResultState.Bye, SSEResultState.Failure, SSEResultState.Ack].forEach((name) => {
          const fName = name.toString();
          this.eventSource.addEventListener(fName,
                                            e => {
        try {
          fhLog.log("received ", fName, JSON.stringify(e));
          this._repository.notify(name, JSON.parse((e as any).data));
        } catch (e) { fhLog.error(JSON.stringify(e)); }
                                        });
    });

    this.eventSource.onerror = (e) => {
      fhLog.error("got error", e);
      this._repository.notify (SSEResultState.Failure, null);
    };
  }

  close() {
    if (this.eventSource != null) {
      this.eventSource.close();
      this.eventSource = null;
    }
  }

  clientEvaluated(): boolean {
    return this._config.clientEvaluated();
  }

  contextChange(header: string): Promise<void> {
    return Promise.resolve(undefined);
  }

  poll(): Promise<void> {
    this.init();

    return new Promise<void>((resolve) => resolve());
  }

  requiresReplacementOnHeaderChange(): boolean {
    return true;
  }
}
