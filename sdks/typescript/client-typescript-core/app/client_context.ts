import {
  StrategyAttributeCountryName,
  StrategyAttributeDeviceName,
  StrategyAttributePlatformName
} from './models/models';
import { FeatureStateHolder } from './feature_state';
import { FeatureHubRepository } from './client_feature_repository';
import { EdgeServiceSupplier, FeatureHubConfig } from './feature_hub_config';
import { EdgeService } from './edge_service';
import { InternalFeatureRepository } from './internal_feature_repository';

export interface ConfigChangedListener {
  (config: ClientContext);
}

export interface ClientContext {
  userKey(value: string): ClientContext;
  sessionKey(value: string): ClientContext;
  country(value: StrategyAttributeCountryName): ClientContext;
  device(value: StrategyAttributeDeviceName): ClientContext;
  platform(value: StrategyAttributePlatformName): ClientContext;
  version(version: string): ClientContext;
  attribute_value(key: string, value: string): ClientContext;
  attribute_values(key: string, values: Array<string>): ClientContext;
  clear(): ClientContext;
  build(): Promise<ClientContext>;

  getAttr(key: string, defaultValue: string): string;
  defaultPercentageKey(): string;

  feature(name: string): FeatureStateHolder;
  isEnabled(name: string): boolean;
  isSet(name: string): boolean;
  edgeService(): EdgeService;
  repository(): FeatureHubRepository;
  logAnalyticsEvent(action: string, user?: string, other?: Map<string, string>);

  close();
}

export abstract class BaseClientContext implements ClientContext {
  protected readonly _repository: InternalFeatureRepository;
  protected _attributes = new Map<string, Array<string>>();
  protected readonly _config: FeatureHubConfig;

  constructor(repository: InternalFeatureRepository, config: FeatureHubConfig) {
    this._repository = repository;
    this._config = config;
  }

  userKey(value: string): ClientContext {
    this._attributes.set('userkey', [value]);
    return this;
  }

  sessionKey(value: string): ClientContext {
    this._attributes.set('session', [value]);
    return this;
  }

  country(value: StrategyAttributeCountryName): ClientContext {
    this._attributes.set('country', [value]);
    return this;
  }

  device(value: StrategyAttributeDeviceName): ClientContext {
    this._attributes.set('device', [value]);
    return this;
  }

  platform(value: StrategyAttributePlatformName): ClientContext {
    this._attributes.set('platform', [value]);
    return this;
  }

  version(version: string): ClientContext {
    this._attributes.set('version', [version]);
    return this;
  }

  attribute_value(key: string, value: string): ClientContext {
    this._attributes.set(key, [value]);
    return this;
  }

  attribute_values(key: string, values: Array<string>): ClientContext {
    this._attributes.set(key, values);
    return this;
  }

  clear(): ClientContext {
    this._attributes.clear();
    return this;
  }

  getAttr(key: string, defaultValue?: string): string {
    if (this._attributes.has(key)) {
      return this._attributes.get(key)[0];
    }

    return defaultValue;
  }

  defaultPercentageKey(): string {
    return this._attributes.has('session') ? this.getAttr('session') : this.getAttr('userkey');
  }


  isEnabled(name: string): boolean {
    return this.feature(name).isEnabled();
  }

  isSet(name: string): boolean {
    return  this.feature(name).isSet();
  }

  abstract async build(): Promise<ClientContext>;

  abstract feature(name: string): FeatureStateHolder;
  // feature(name: string): FeatureStateHolder {
  //   return this._repository.feature(name);
  // }
  abstract edgeService(): EdgeService;
  abstract close();

  repository(): FeatureHubRepository {
    return this._repository;
  }

  logAnalyticsEvent(action: string, user?: string, other?: Map<string, string>) {
    if (user == null) {
      user = this.getAttr('userkey');
    }
    if (user != null) {
      if (other == null) {
        other = new Map<string, string>();
      }

      other.set('cid', user);
    }

    this._repository.logAnalyticsEvent(action, other);
  }
}

export class ServerEvalFeatureContext extends BaseClientContext {
  private  _edgeServiceSupplier: EdgeServiceSupplier;
  private _currentEdge: EdgeService;
  private _xHeader: string;

  constructor(repository: InternalFeatureRepository, config: FeatureHubConfig,
              edgeServiceSupplier: EdgeServiceSupplier) {
    super(repository, config);

    this._edgeServiceSupplier = edgeServiceSupplier;
  }

  async build(): Promise<ClientContext> {
    const newHeader = Array.from(this._attributes.entries()).map((key,
       ) =>
         key[0] + '=' + encodeURIComponent(key[1].join(','))).sort().join(',');

    if (newHeader !== this._xHeader) {
      this._xHeader = newHeader;
      this._repository.notReady();

      if (this._currentEdge != null && this._currentEdge.requiresReplacementOnHeaderChange()) {
        this._currentEdge.close();
        this._currentEdge = null;
      }
    }

    if (this._currentEdge == null) {
      this._currentEdge = this._edgeServiceSupplier();
    }

    await this._currentEdge.contextChange(this._xHeader);

    return this;
  }

  close() {
    if (this._currentEdge) {
      this._currentEdge.close();
    }
  }

  edgeService(): EdgeService {
    return this._currentEdge;
  }

  feature(name: string): FeatureStateHolder {
    return this._repository.feature(name);
  }
}

export class ClientEvalFeatureContext extends BaseClientContext {
  private readonly _edgeService: EdgeService;

  constructor(repository: InternalFeatureRepository, config: FeatureHubConfig, edgeService: EdgeService) {
    super(repository, config);

    this._edgeService = edgeService;
  }

  async build(): Promise<ClientContext> {
    return this;
  }

  close() {
    this._edgeService.close();
  }

  edgeService(): EdgeService {
    return this._edgeService;
  }

  feature(name: string): FeatureStateHolder {
    return this._repository.feature(name).withContext(this);
  }

}
