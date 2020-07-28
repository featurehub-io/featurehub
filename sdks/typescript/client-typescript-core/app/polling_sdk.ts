import { ClientFeatureRepository } from './client_feature_repository';
import { Environment, FeatureState, ObjectSerializer, SSEResultState } from './models/models';

interface PollingService {

  start(): void;

  stop(): void;
}

type FeaturesFunction = (environments: Array<Environment>) => void;

class PollingBase {
  options: BrowserOptions;
  url: string;
  frequency: number;
  callback: FeaturesFunction;
  stopped: boolean = false;

  constructor(options: BrowserOptions, url: string, frequency: number, callback: FeaturesFunction) {
    this.options = options;
    this.url = url;
    this.frequency = frequency;
    this.callback = callback;
  }
}

class BrowserPollingService extends PollingBase implements PollingService {

  constructor(options: BrowserOptions, url: string, frequency: number, callback: FeaturesFunction) {
    super(options, url, frequency, callback);
  }

  start(): void {
    this.poll();
  }

  public stop(): void {
    this.stopped = true;
  }

  private poll(): void {
    const req = new XMLHttpRequest();
    req.open('GET', this.url);
    req.setRequestHeader('Content-type', 'application/json');
    req.send();
    req.onreadystatechange = () => {
      if (req.readyState === 4) {
        if (req.status === 200) {
          this.callback(ObjectSerializer.deserialize(JSON.parse(req.responseText), 'Array<Environment>'));
        }
        if (req.status !== 400) {
          this.delayTimer();
        }
      }
    };
  }

  private delayTimer(): void {
    if (!this.stopped) {
      window.setTimeout(() => this.poll(), this.frequency);
    }
  }
}

class NodejsPollingService implements PollingService {
  start(): void {
  }

  stop(): void {
  }
}

export interface BrowserOptions {

}

export interface NodejsOptions {

}

export class FeatureHubPollingClient {
  private _frequency: number;
  private _url: string;
  private _repository: ClientFeatureRepository;
  private _pollingService: PollingService | undefined;
  private _options: BrowserOptions|NodejsOptions;
  private _startable: boolean;

  constructor(repository: ClientFeatureRepository, host: string, frequency: number,
              envIds: Array<string>, options: BrowserOptions|NodejsOptions = {}) {
    this._frequency = frequency;
    this._repository = repository;
    this._options = options;
    this._startable = envIds.length !== 0;
    this._url = host + '/features?' + envIds.map(e => 'sdkUrl=' + encodeURIComponent(e)).join('&');
  }

  public start() {
    if (this._pollingService === undefined) {
      if (typeof window === 'object') {
        this._pollingService = new BrowserPollingService(this._options, this._url, this._frequency,
          (e) => this.response(e));
      } else {
        this._pollingService = new NodejsPollingService();
      }

      this._pollingService.start();
    }
  }

  public stop() {
    this._pollingService.stop();
    this._pollingService = undefined;
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
          features.push(... e.features);
        }
      });

      this._repository.notify(SSEResultState.Features, features);
    }
  }

}
