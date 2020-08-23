import { StrategyAttributeCountryName, StrategyAttributeDeviceName, StrategyAttributePlatformName } from './models/models';

export interface ConfigChangedListener {
  (config: FeatureHubConfig);
}

export interface ConfigChangedListenerRemove {
  ();
}

export class FeatureHubConfig {
  private _attributes = new Map<String, Array<String>>();
  private _listeners: Array<ConfigChangedListener> = [];

  userKey(value: String): FeatureHubConfig {
    this._attributes.set('userkey', [value]);
    return this;
  }

  sessionKey(value: String): FeatureHubConfig {
    this._attributes.set('session', [value]);
    return this;
  }

  country(value: StrategyAttributeCountryName): FeatureHubConfig {
    this._attributes.set('country', [value]);
    return this;
  }

  device(value: StrategyAttributeDeviceName): FeatureHubConfig {
    this._attributes.set('device', [value]);
    return this;
  }

  platform(value: StrategyAttributePlatformName): FeatureHubConfig {
    this._attributes.set('platform', [value]);
    return this;
  }

  attribute_value(key: string, value: string): FeatureHubConfig {
    this._attributes.set(key, [value]);
    return this;
  }

  attribute_values(key: string, values: Array<string>): FeatureHubConfig {
    this._attributes.set(key, values);
    return this;
  }

  async build() {
    for (let l of this._listeners) {
      (async () => {
        try {
          l(this);
          // tslint:disable-next-line:no-empty
        } catch (e) {
        }
      })();
    }
  }

  registerChangeListener(cl: ConfigChangedListener): ConfigChangedListenerRemove {
    this._listeners.push(cl);
    // call it first up so it doesn't have to special case logic to update the header.
    try {
      cl(this);
      // tslint:disable-next-line:no-empty
    } catch (e) {}
    return () => {
      const pos = this._listeners.indexOf(cl);
      if (pos >= 0) {
        this._listeners.splice(pos, 1);
      }
    };
  }

  // we follow the W3C Baggage spec style for encoding
  generateHeader(): string {
    if (this._attributes.size === 0) { return undefined; }

    return Array.from(this._attributes.entries()).map((key,
                                                ) =>
      key[0] + '=' + encodeURIComponent(key[1].join(','))).join(',');

  }
}
