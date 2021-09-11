/* eslint-disable */
import { EdgeService } from './edge_service';
import { FeatureHubConfig, fhLog } from './feature_hub_config';
import { InternalFeatureRepository } from './internal_feature_repository';
import { SSEResultState } from './models';
import { Readyness } from './featurehub_repository';

export declare class EventSource {
  static readonly CLOSED: number;
  static readonly CONNECTING: number;
  static readonly OPEN: number;

  readonly CLOSED: number;
  readonly CONNECTING: number;
  readonly OPEN: number;
  readonly url: string;
  readonly readyState: number;
  readonly withCredentials: boolean;

  onopen: (evt: MessageEvent) => any;
  onmessage: (evt: MessageEvent) => any;
  onerror: (evt: MessageEvent) => any;

  constructor(url: string, eventSourceInitDict?: EventSource.EventSourceInitDict);

  addEventListener(type: string, listener: EventListener): void;
  dispatchEvent(evt: Event): boolean;
  removeEventListener(type: string, listener?: EventListener): void;
  close(): void;
}

// eslint-disable-next-line @typescript-eslint/no-namespace
export declare namespace EventSource {
  enum ReadyState { CONNECTING = 0, OPEN = 1, CLOSED = 2 }

  interface EventSourceInitDict {
    withCredentials?: boolean;
    // eslint-disable-next-line @typescript-eslint/ban-types
    headers?: object;
    proxy?: string;
    // eslint-disable-next-line @typescript-eslint/ban-types
    https?: object;
    rejectUnauthorized?: boolean;
  }
}

export type EventSourceProvider = (url: string, eventSourceInitDict?: EventSource.EventSourceInitDict) => EventSource;

export class FeatureHubEventSourceClient implements EdgeService {
  private eventSource: EventSource;
  private readonly _config: FeatureHubConfig;
  private readonly _repository: InternalFeatureRepository;
  private _header: string;

  public static eventSourceProvider: EventSourceProvider = (url, dict) => {
    const realUrl = dict.headers && dict.headers['x-featurehub'] ?
      url + '?xfeaturehub=' + encodeURI(dict.headers['x-featurehub']) : url;
    return new EventSource(realUrl, dict);
  };

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

    this.eventSource = FeatureHubEventSourceClient.eventSourceProvider(this._config.url(), options);

    [SSEResultState.Features, SSEResultState.Feature, SSEResultState.DeleteFeature,
      SSEResultState.Bye, SSEResultState.Failure, SSEResultState.Ack].forEach((name) => {
      const fName = name.toString();
      this.eventSource.addEventListener(fName,
        e => {
          try {
            const data = JSON.parse((e as any).data);
            fhLog.trace(`received ${fName}`, data);
            this._repository.notify(name, data);
          } catch (e) { fhLog.error(JSON.stringify(e)); }
        });
    });

    this.eventSource.onerror = (e) => {
      if (this._repository.readyness !== Readyness.Ready) {
        fhLog.error('Connection failed and repository not in ready state indicating persistent failure', e);
        this._repository.notify (SSEResultState.Failure, null);
      } else {
        fhLog.trace('refreshing connection in case of staleness');
      }
    };
  }

  close() {
    if (this.eventSource != null) {
      const es = this.eventSource;
      this.eventSource = undefined;
      es.close();
    }
  }

  clientEvaluated(): boolean {
    return this._config.clientEvaluated();
  }

  contextChange(header: string): Promise<void> {
    this._header = header;

    if (this.eventSource !== undefined) {
      this.close();
    }

    this.init();

    return Promise.resolve(undefined);
  }

  poll(): Promise<void> {
    if (this.eventSource === undefined) {
      this.init();
    }

    return new Promise<void>((resolve) => resolve());
  }

  requiresReplacementOnHeaderChange(): boolean {
    return true;
  }
}
