import { ClientFeatureRepository } from './client_feature_repository';
import { Environment, FeatureState, ObjectSerializer, SSEResultState } from './models/models';

interface PollingService {

  start(): void;

  stop(): void;

  forcePoll(): void; // occurs if config changed

  attributeHeader(header: string): void;
}

type FeaturesFunction = (environments: Array<Environment>) => void;

abstract class PollingBase implements PollingService {
  _header: string;
  options: BrowserOptions;
  url: string;
  frequency: number;
  callback: FeaturesFunction;
  stopped: boolean = false;

  constructor(options: BrowserOptions, url: string, frequency: number, fhheader: string, callback: FeaturesFunction) {
    this.options = options;
    this.url = url;
    this.frequency = frequency;
    this.callback = callback;
    this._header = fhheader;
  }

  attributeHeader(header: string) {
    if (header !== this._header) {
      this.forcePoll();
    }
  }

  abstract start(): void;

  abstract stop(): void;

  abstract forcePoll(): void;
}

class BrowserPollingService extends PollingBase implements PollingService {
  private polling: boolean;

  constructor(options: BrowserOptions, url: string, frequency: number, fhheader: string, callback: FeaturesFunction) {
    super(options, url, frequency, fhheader, callback);
  }

  start(): void {
    this.poll();
  }

  public stop(): void {
    this.stopped = true;
  }

  public forcePoll(): void {
    if (!this.polling) {
      this.poll();
    }
  }

  private poll(): void {
    if (this.polling) {
      this.delayTimer();
    }

    this.polling = true;

    const req = new XMLHttpRequest();
    req.open('GET', this.url);
    req.setRequestHeader('Content-type', 'application/json');
    if (this._header) {
      req.setRequestHeader('x-featurehub', this._header);
    }
    req.send();
    req.onreadystatechange = () => {
      if (req.readyState === 4) {
        this.polling = false;

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
    if (!this.stopped && this.frequency > 0) {
      window.setTimeout(() => this.poll(), this.frequency);
    }
  }

}

class NodejsPollingService extends PollingBase implements PollingService {
  private uri: URL;
  private polling = false;

  constructor(options: BrowserOptions, url: string, frequency: number, fhheader: string, callback: FeaturesFunction) {
    super(options, url, frequency, fhheader, callback);

    this.uri = new URL(this.url);
  }

  start(): void {
    this.poll();
  }

  stop(): void {
    this.stopped = true;
  }

  public forcePoll(): void {
    if (!this.polling) {
      this.poll();
    }
  }

  private poll(): void {
    if (this.polling) {
      this.delayTimer();
    }

    this.polling = true;

    const http = this.uri.protocol === 'http:' ? require('http') : require('https');
    let data = '';
    let headers = this._header === undefined ? {} : {
      'x-featurehub': this._header
    };
    const reqOptions = {
      protocol: this.uri.protocol,
      host: this.uri.host,
      hostname: this.uri.hostname,
      port: this.uri.port,
      method: 'GET',
      path: this.uri.pathname,
      headers: headers,
      timeout: this.options.timeout || 8000
    };
    const req = http.request(reqOptions, (res) => {
      res.on('data', (chunk) => data += chunk);
      res.on('end', () => {
        this.polling = false;

        if (res.statusCode === 200) {
          this.callback(ObjectSerializer.deserialize(JSON.parse(data), 'Array<Environment>'));
        }

        if (res.statusCode !== 400) {
          this.delayTimer();
        }
      });
    });

    req.end();
  }

  private delayTimer() {
    if (!this.stopped && this.frequency > 0) {
      setTimeout( () => this.poll(), this.frequency);
    }
  }
}

export interface BrowserOptions {
  timeout?: number;
}

export interface NodejsOptions {
  timeout?: number;
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

    // backwards compatible
    if (repository.clientContext) {
      repository.clientContext.registerChangeListener(() => {
        if (this._pollingService && this._frequency) {
          this._pollingService.attributeHeader(this._repository.clientContext.generateHeader());
        }
      });
    }
  }

  public start() {
    if (this._pollingService === undefined) {
      if (typeof window === 'object') {
        this._pollingService = new BrowserPollingService(this._options, this._url, this._frequency,
                                                         this._repository.clientContext.generateHeader(),
                                                         (e) => this.response(e));
      } else {
        this._pollingService = new NodejsPollingService(this._options, this._url, this._frequency,
                                                        this._repository.clientContext.generateHeader(),
                                                        (e) => this.response(e));
      }
    }

    this._pollingService.start();
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
          // set the environment id so each feature knows which environment it comes from
          e.features.forEach(f => {
            f.environmentId = e.id;
          });
          features.push(... e.features);
        }
      });

      this._repository.notify(SSEResultState.Features, features);
    }
  }

}
