import { FeatureStateUpdate, ObjectSerializer } from './models/models';

interface FeatureUpdatePostManager {
  post(url: string, update: FeatureStateUpdate) : Promise<boolean>;
}

class BrowserFeaturePostUpdater implements FeatureUpdatePostManager{
  post(url: string, update: FeatureStateUpdate): Promise<boolean> {
    return new Promise<boolean>( (resolve) => {
      const req = new XMLHttpRequest();
      req.open('PUT', url);
      req.setRequestHeader('Content-type', 'application/json');
      req.send(JSON.stringify(ObjectSerializer.serialize(update, 'FeatureStateUpdate')));
      req.onreadystatechange  = function() {
        if (req.readyState === 4) {
          resolve(req.status === 200 || req.status === 201);
        }
      };
    });
  }
}

class NodejsFeaturePostUpdater implements FeatureUpdatePostManager {
  post(url: string, update: FeatureStateUpdate): Promise<boolean> {
    const loc = new URL(url);
    const cra = {protocol: loc.protocol, path: loc.pathname,
      host: loc.host, method: 'PUT', port: loc.port, timeout: 3000,
      headers: {
        'content-type': 'application/json'
      }
    };
    const http = cra.protocol === 'http:' ? require('http') : require('https');
    return new Promise<boolean>((resolve, reject) => {
      try {
        const req = http.request(cra, (res) => {
          if (res.statusCode === 200 || res.statusCode === 201) {
            resolve(true);
          } else {
            resolve(false);
          }
        });

        req.on('error', (e) => {
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
