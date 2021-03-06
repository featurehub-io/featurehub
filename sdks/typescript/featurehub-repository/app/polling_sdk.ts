import { Environment, FeatureState, ObjectSerializer, SSEResultState } from './models/models';
import { EdgeService } from './edge_service';
import { FeatureHubConfig, fhLog } from './feature_hub_config';
import { InternalFeatureRepository } from './internal_feature_repository';

interface PollingService {

  poll(): Promise<void>;

  stop(): void;

  attributeHeader(header: string): Promise<void>;
}

type FeaturesFunction = (environments: Array<Environment>) => void;

abstract class PollingBase implements PollingService {
  protected url: string;
  protected frequency: number;
  protected _callback: FeaturesFunction;
  protected stopped: boolean = false;
  protected _header: string;

  constructor(url: string, frequency: number, callback: FeaturesFunction) {
    this.url = url;
    this.frequency = frequency;
    this._callback = callback;
  }

  async attributeHeader(header: string): Promise<void> {
    this._header = header;
    return this.poll();
  }

  public stop(): void {
    this.stopped = true;
  }

  public abstract poll(): Promise<void>;

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

class BrowserPollingService extends PollingBase implements PollingService {
  private readonly _options: BrowserOptions;

  constructor(options: BrowserOptions, url: string, frequency: number, callback: FeaturesFunction) {
    super(url, frequency, callback);

    this._options = options;
  }

  public async poll(): Promise<void> {
    return new Promise((resolve, reject) => {
      const req = new XMLHttpRequest();
      req.open('GET', this.url);
      req.setRequestHeader('Content-type', 'application/json');
      if (this._header) {
        req.setRequestHeader('x-featurehub', this._header);
      }

      req.send();

      req.onreadystatechange = () => {
        if (req.readyState === 4) {
          if (req.status === 200) {
            this._callback(ObjectSerializer.deserialize(JSON.parse(req.responseText), 'Array<Environment>'));
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

class NodejsPollingService extends PollingBase implements PollingService {
  private readonly uri: URL;
  private readonly _options: NodejsOptions;

  constructor(options: NodejsOptions, url: string, frequency: number, _callback: FeaturesFunction) {
    super(url, frequency, _callback);

    this._options = options;
    this.uri = new URL(this.url);
  }

  public async poll(): Promise<void> {
    return new Promise(((resolve, reject) => {
      const http = this.uri.protocol === 'http:' ? require('http') : require('https');
      let data = '';
      let headers = this._header === undefined ? {} : {
        'x-featurehub': this._header
      };
      // we are not specifying the type as it forces us to bring in one of http or https
      const reqOptions = {
        protocol: this.uri.protocol,
        host: this.uri.host,
        hostname: this.uri.hostname,
        port: this.uri.port,
        method: 'GET',
        path: this.uri.pathname + this.uri.search,
        headers: headers,
        timeout: this._options.timeout || 8000
      };
      const req = http.request(reqOptions, (res) => {
        res.on('data', (chunk) => data += chunk);
        res.on('end', () => {
          if (res.statusCode === 200) {
            this._callback(ObjectSerializer.deserialize(JSON.parse(data), 'Array<Environment>'));
            resolve();
          } else if (res.statusCode >= 400 && res.statusCode < 500) {
            reject(`Failed to connect to ${this.url} with ${res.statusCode}`);
          } else {
            this.delayTimer().then(resolve).catch(reject);
          }
        });
      });

      req.end();
    }));
  }

}

export interface BrowserOptions {
  timeout?: number;
}

export interface NodejsOptions {
  timeout?: number;
}

export class FeatureHubPollingClient implements EdgeService {
  private _frequency: number;
  private _url: string;
  private _repository: InternalFeatureRepository;
  private _pollingService: PollingService | undefined;
  private _options: BrowserOptions | NodejsOptions;
  private _startable: boolean;
  private readonly _config: FeatureHubConfig;
  private _xHeader: string;

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

  _initService() {
    if (this._pollingService === undefined) {
      if (Object.prototype.toString.call(global.process) !== '[object process]') {
        this._pollingService = new BrowserPollingService(this._options, this._url, this._frequency,
          (e) => this.response(e));
      } else {
        this._pollingService = new NodejsPollingService(this._options, this._url, this._frequency,
          (e) => this.response(e));
      }

      fhLog.log(`featurehub: initialized polling client to ${this._url}`);
    }
  }

  async contextChange(header: string): Promise<void> {
    if (!this._config.clientEvaluated()) {
      if (this._xHeader !== header) {
        this._xHeader = header;

        this._initService();
        return this._pollingService.attributeHeader(header);
      }
    }
  }

  clientEvaluated(): boolean {
    return this._config.clientEvaluated();
  }

  requiresReplacementOnHeaderChange(): boolean {
    return false;
  }

  close(): void {
    if (this._pollingService) {
      this._pollingService.stop();
    }
  }

  poll(): Promise<void> {
    this._initService();

    return this._pollingService.poll();
  }

  private stop() {
    this._pollingService.stop();
    this._pollingService = undefined;
  }

  private _restartTimer() {
    setTimeout(() => this._pollingService.poll().catch((e) => {
      // we only get here if we failed once, so lets assume it is transient and keep going
      // console.error(e);
      fhLog.error(`Failed to poll, restarting in ${this._frequency}ms: ${e}`);
      this._repository.notify(SSEResultState.Failure,  null);
    }).finally(() => this._restartTimer()), this._frequency);
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
      this._restartTimer();
    }
  }

}
