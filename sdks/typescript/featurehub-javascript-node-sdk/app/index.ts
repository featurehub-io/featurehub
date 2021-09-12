import {
  FeatureHubPollingClient,
  FeatureStateUpdate, FeatureUpdatePostManager, FeatureUpdater, GoogleAnalyticsApiClient, GoogleAnalyticsCollector,
  NodejsOptions,
  ObjectSerializer,
  PollingBase,
  FeatureHubEventSourceClient,
  PollingService, FeaturesFunction
} from 'featurehub-javascript-client-sdk';

const ES = require('eventsource');

export * from 'featurehub-javascript-client-sdk';

FeatureHubEventSourceClient.eventSourceProvider = (url, dict) => {
  return new ES(url, dict);
};

class NodejsPollingService extends PollingBase implements PollingService {
  private readonly uri: URL;
  private readonly _options: NodejsOptions;

  constructor(options: NodejsOptions, url: string, frequency: number, _callback: FeaturesFunction) {
    super(url, frequency, _callback);

    this._options = options;
    this.uri = new URL(this.url);
  }

  public poll(): Promise<void> {
    return new Promise(((resolve, reject) => {
      const http = this.uri.protocol === 'http:' ? require('http') : require('https');
      let data = '';
      const headers = this._header === undefined ? {} : {
        'x-featurehub': this._header
      };

      if (this._etag) {
        headers['if-none-match'] = this._etag;
      }

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
            this._etag = res.headers.etag;
            this._callback(ObjectSerializer.deserialize(JSON.parse(data), 'Array<Environment>'));
            resolve();
          } else if (res.statusCode == 304) {
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

FeatureHubPollingClient.pollingClientProvider = (opt, url, freq, callback) =>
  new NodejsPollingService(opt, url, freq, callback);

class NodejsFeaturePostUpdater implements FeatureUpdatePostManager {
  post(url: string, update: FeatureStateUpdate): Promise<boolean> {
    const loc = new URL(url);
    const cra = { protocol: loc.protocol, path: loc.pathname,
      host: loc.hostname, method: 'PUT', port: loc.port, timeout: 3000,
      headers: {
        'content-type': 'application/json'
      }
    };
    const http = cra.protocol === 'http:' ? require('http') : require('https');
    return new Promise<boolean>((resolve) => {
      try {
        const req = http.request(cra, (res) => {
          if (res.statusCode >= 200 && res.statusCode < 300) {
            resolve(true);
          } else {
            resolve(false);
          }
        });

        req.on('error', () => {
          resolve(false);
        });

        const data = ObjectSerializer.serialize(update, 'FeatureStateUpdate');
        req.write(JSON.stringify(data));
        req.end();
      } catch (e) {
        resolve(false);
      }
    });
  }

}

FeatureUpdater.featureUpdaterProvider = () => new NodejsFeaturePostUpdater();

class NodejsGoogleAnalyticsApiClient implements GoogleAnalyticsApiClient {
  cid(other: Map<string, string>): string {
    return other.get('cid') || process.env.GA_CID;
  }

  postBatchUpdate(batchData: string): void {
    const req = require('https').request({
      host: 'www.google-analytics.com',
      path: 'batch'
    });
    req.write(batchData);
    req.end();
  }
}

GoogleAnalyticsCollector.googleAnalyticsClientProvider = () => new NodejsGoogleAnalyticsApiClient();
