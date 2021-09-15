// prevents circular deps
import { ObjectSerializer } from './models/models/model_serializer';

import { Environment, FeatureState, SSEResultState } from './models';
import { EdgeService } from './edge_service';
import { FeatureHubConfig, fhLog } from './feature_hub_config';
import { InternalFeatureRepository } from './internal_feature_repository';

export interface PollingService {

  poll(): Promise<void>;

  stop(): void;

  attributeHeader(header: string): Promise<void>;
}

export type FeaturesFunction = (environments: Array<Environment>) => void;

export abstract class PollingBase implements PollingService {
  protected url: string;
  protected frequency: number;
  protected _callback: FeaturesFunction;
  protected stopped = false;
  protected _header: string;
  protected _etag: string;

  constructor(url: string, frequency: number, callback: FeaturesFunction) {
    this.url = url;
    this.frequency = frequency;
    this._callback = callback;
  }

  attributeHeader(header: string): Promise<void> {
    this._header = header;
    return this.poll();
  }

  public stop(): void {
    this.stopped = true;
  }

  public abstract poll(): Promise<void>;

  // eslint-disable-next-line require-await
  protected async delayTimer(): Promise<void> {
    return new Promise(((resolve, reject) => {
      if (!this.stopped && this.frequency > 0) {
        setTimeout(() => this.poll().then(resolve).catch(reject), this.frequency);
      } else {
        resolve();
      }
    }));
  }
}

export interface NodejsOptions {
  timeout?: number;
}

export interface BrowserOptions {
  timeout?: number;
}

class BrowserPollingService extends PollingBase implements PollingService {
  private readonly _options: BrowserOptions;

  constructor(options: BrowserOptions, url: string, frequency: number, callback: FeaturesFunction) {
    super(url, frequency, callback);

    this._options = options;
  }

  public poll(): Promise<void> {
    return new Promise((resolve, reject) => {
      const req = new XMLHttpRequest();
      req.open('GET', this.url);
      req.setRequestHeader('Content-type', 'application/json');

      if (this._etag) {
        req.setRequestHeader('if-none-match', this._etag);
      }

      if (this._header) {
        req.setRequestHeader('x-featurehub', this._header);
      }

      req.send();

      req.onreadystatechange = () => {
        if (req.readyState === 4) {
          if (req.status === 200) {
            this._etag = req.getResponseHeader('etag');
            this._callback(ObjectSerializer.deserialize(JSON.parse(req.responseText), 'Array<Environment>'));
            resolve();
          } else if (req.status == 304) { // no change
            resolve();
          } else if (req.status >= 400 && req.status < 500) {
            reject(`Failed to connect to ${this.url} with ${req.status}`);
          } else {
            this.delayTimer().then(resolve).catch(reject);
          }
        }
      };
    });
  }
}

export type PollingClientProvider = (options: BrowserOptions, url: string,
                                     frequency: number, callback: FeaturesFunction) => PollingBase;

export class FeatureHubPollingClient implements EdgeService {
  private _frequency: number;
  private _url: string;
  private _repository: InternalFeatureRepository;
  private _pollingService: PollingService | undefined;
  private _options: BrowserOptions | NodejsOptions;
  private _startable: boolean;
  private readonly _config: FeatureHubConfig;
  private _xHeader: string;
  private _pollPromiseResolve: (value: (PromiseLike<void> | void)) => void;
  private _pollPromiseReject: (reason?: any) => void;
  private _pollingStarted = false;

  public static pollingClientProvider: PollingClientProvider = (opt, url, freq, callback) =>
    new BrowserPollingService(opt, url, freq, callback);

  constructor(repository: InternalFeatureRepository,
    config: FeatureHubConfig,
    frequency: number,
    options: BrowserOptions | NodejsOptions = {}) {
    this._frequency = frequency;
    this._repository = repository;
    this._options = options;
    this._config = config;
    this._url = config.getHost() + 'features?' + config.getApiKeys().map(e => 'sdkUrl=' + encodeURIComponent(e)).join('&');
  }

  private _initService(): void {
    if (this._pollingService === undefined) {
      this._pollingService =
        FeatureHubPollingClient.pollingClientProvider(this._options, this._url,
          this._frequency,
          (e) =>
            this.response(e));

      fhLog.log(`featurehub: initialized polling client to ${this._url}`);
    }
  }

  public contextChange(header: string): Promise<void> {
    if (!this._config.clientEvaluated()) {
      if (this._xHeader !== header) {
        this._xHeader = header;

        this._initService();

        const pollForContext = this._pollingService.attributeHeader(header);

        if (!this._pollingStarted) {
          this._restartTimer();
        }

        return pollForContext;
      }
    } else {
      return new Promise<void>((resolve) => resolve());
    }
  }

  public clientEvaluated(): boolean {
    return this._config.clientEvaluated();
  }

  public requiresReplacementOnHeaderChange(): boolean {
    return false;
  }

  public close(): void {
    if (this._pollingService) {
      this._pollingService.stop();
    }
  }

  public poll(): Promise<void> {
    if (this._pollPromiseResolve !== undefined || this._pollingStarted) {
      return new Promise<void>((resolve) => resolve());
    }

    this._initService();

    return new Promise<void>((resolve, reject) => {
      this._pollPromiseReject = reject;
      this._pollPromiseResolve = resolve;

      this._restartTimer();
    });
  }

  private stop() {
    this._pollingService.stop();
    this._pollingService = undefined;
  }

  private _restartTimer() {
    this._pollingStarted = true;
    setTimeout(() => this._pollingService.poll()
      .then(() => {
        if (this._pollPromiseResolve !== undefined) {
          this._pollPromiseResolve();
        }
      })
      .catch((e) => {
        // we only get here if we failed once, so lets assume it is transient and keep going
        // console.error(e);
        fhLog.error(`Failed to poll, restarting in ${this._frequency}ms: ${e}`);
        this._repository.notify(SSEResultState.Failure, null);
        if (this._pollPromiseReject !== undefined) {
          this._pollPromiseReject();
        }
      }).finally(() => {
        this._pollPromiseReject = undefined;
        this._pollPromiseResolve = undefined;
        this._restartTimer();
      }),        this._frequency);
  }

  private response(environments: Array<Environment>): void {
    if (environments.length === 0) {
      this._startable = false;
      this.stop();
      this._repository.notify(SSEResultState.Failure, null);
    } else {
      const features = new Array<FeatureState>();

      environments.forEach(e => {
        if (e.features.length > 0) {
          // set the environment id so each feature knows which environment it comes from
          e.features.forEach(f => {
            f.environmentId = e.id;
          });
          features.push(...e.features);
        }
      });

      this._repository.notify(SSEResultState.Features, features);
    }
  }

}
