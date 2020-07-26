import { FeatureStateUpdate, ObjectSerializer } from './models/models';

interface FeatureUpdatePostManager {
  post(url: string, update: FeatureStateUpdate) : Promise<boolean>;
}

class BrowserFeaturePostUpdater implements FeatureUpdatePostManager{
  post(url: string, update: FeatureStateUpdate): Promise<boolean> {
    const req = new XMLHttpRequest();
    req.open('POST', url);
    req.setRequestHeader('content-type', 'application/json');
    req.send(ObjectSerializer.serialize(update, 'FeatureStateUpdate'));

    return Promise.resolve(true);
  }
}

class NodejsFeaturePostUpdater implements FeatureUpdatePostManager {
  post(url: string, update: FeatureStateUpdate): Promise<boolean> {
    const loc = new URL(url);
    const cra = {protocol: loc.protocol, path: loc.pathname, host: loc.host, method: 'PUT', port: loc.port};
    const http = cra.protocol == 'http:' ? require('http') : require('https');
    return new Promise<boolean>((resolve, reject) => {
      try {
        const req = http.request(cra, (res) => {
          if (res.statusCode === 200 || res.statusCode === 201) {
            resolve(true);
          } else {
            resolve(false);
          }
        });

        req.setHeader('content-type', 'application/json');
        req.write(ObjectSerializer.serialize(update, 'FeatureStateUpdate'));
        req.end();
      } catch (e) {
        resolve(false);
      }
    });
  }

}

export class FeatureUpdater {
  private sdkUrl: string;
  private manager: FeatureUpdatePostManager;

  constructor(sdkUrl: string) {
    this.sdkUrl = sdkUrl;

    if (typeof window === 'object') {
      this.manager = new BrowserFeaturePostUpdater();
    } else {
      this.manager = new NodejsFeaturePostUpdater();
    }
  }

  public updateKey(key: string, update: FeatureStateUpdate): Promise<boolean> {
    return this.manager.post(this.sdkUrl + '/' + key, update);
  }
}

const fu = new FeatureUpdater('https://kasd.demo.featurehub.io/features/default/15086f5a-03b7-4c9a-861e-62aa5d50e64c/0UG58dDK28eNr6cY1kEZjL9gDla2n7H3UgTehwydTIvCQRvIj5KNWEN1LAFdVzYj8bVyclnB4wUn7iQU');

fu.updateKey('meep', new FeatureStateUpdate({lock: false, value: true})).then((r) => console.log('result is', r));
