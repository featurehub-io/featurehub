import { FeatureStateUpdate } from './models';
// prevents circular deps
import { ObjectSerializer } from './models/models/model_serializer';
import { FeatureHubConfig } from './feature_hub_config';

export interface FeatureUpdatePostManager {
  post(url: string, update: FeatureStateUpdate): Promise<boolean>;
}

class BrowserFeaturePostUpdater implements FeatureUpdatePostManager {
  post(url: string, update: FeatureStateUpdate): Promise<boolean> {
    return new Promise<boolean>((resolve) => {
      const req = new XMLHttpRequest();
      req.open('PUT', url);
      req.setRequestHeader('Content-type', 'application/json');
      req.send(JSON.stringify(ObjectSerializer.serialize(update, 'FeatureStateUpdate')));
      req.onreadystatechange  = function () {
        if (req.readyState === 4) {
          resolve(req.status >= 200 && req.status < 300);
        }
      };
    });
  }
}

export type FeatureUpdaterProvider = () => FeatureUpdatePostManager;

export class FeatureUpdater {
  private sdkUrl: string;
  private manager: FeatureUpdatePostManager;

  public static featureUpdaterProvider: FeatureUpdaterProvider = () => new BrowserFeaturePostUpdater();

  constructor(config: FeatureHubConfig) {
    this.sdkUrl = config.url();

    this.manager = FeatureUpdater.featureUpdaterProvider();
  }

  public updateKey(key: string, update: FeatureStateUpdate): Promise<boolean> {
    return this.manager.post(this.sdkUrl + '/' + key, update);
  }
}
