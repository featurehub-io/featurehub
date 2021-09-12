import { InternalFeatureRepository } from './internal_feature_repository';
import { EdgeServiceSupplier, fhLog } from './feature_hub_config';
import {
  StrategyAttributeCountryName,
  StrategyAttributeDeviceName,
  StrategyAttributePlatformName,
} from './models';
import { FeatureStateHolder } from './feature_state';
import { EdgeService } from './edge_service';
import { FeatureHubRepository } from './featurehub_repository';
import { ClientContext } from './client_context';

export abstract class BaseClientContext implements ClientContext {
  protected readonly _repository: InternalFeatureRepository;
  protected _attributes = new Map<string, Array<string>>();

  protected constructor(repository: InternalFeatureRepository) {
    this._repository = repository;
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

  // eslint-disable-next-line camelcase
  attribute_value(key: string, value: string): ClientContext {
    this._attributes.set(key, [value]);
    return this;
  }

  // eslint-disable-next-line camelcase
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

  getNumber(name: string): number | undefined {
    return this.feature(name).getNumber();
  }

  getString(name: string): string | undefined {
    return this.feature(name).getString();
  }

  getJson(name: string): any | undefined {
    const val = this.feature(name).getRawJson();
    return val === undefined ? undefined : JSON.parse(val);
  }

  getRawJson(name: string): string | undefined {
    return this.feature(name).getRawJson();
  }

  getFlag(name: string): boolean | undefined {
    return this.feature(name).getFlag();
  }

  getBoolean(name: string): boolean | undefined {
    return this.feature(name).getBoolean();
  }

  abstract build(): Promise<ClientContext>;

  abstract feature(name: string): FeatureStateHolder;
  // feature(name: string): FeatureStateHolder {
  //   return this._repository.feature(name);
  // }
  abstract close(): void;

  repository(): FeatureHubRepository {
    return this._repository;
  }

  logAnalyticsEvent(action: string, other?: Map<string, string>, user?: string): void {
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
  private readonly _edgeServiceSupplier: EdgeServiceSupplier;
  private _currentEdge: EdgeService;
  private _xHeader: string;

  constructor(repository: InternalFeatureRepository,
    edgeServiceSupplier: EdgeServiceSupplier) {
    super(repository);

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

    await this._currentEdge.contextChange(this._xHeader).catch((e) => fhLog.error(`Failed to connect to FeatureHub Edge to refresh context ${e}`));

    return this;
  }

  close(): void {
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

  constructor(repository: InternalFeatureRepository, edgeService: EdgeService) {
    super(repository);

    this._edgeService = edgeService;
  }

  // eslint-disable-next-line require-await
  async build(): Promise<ClientContext> {
    this._edgeService.poll(); // in case it hasn't already been initialized

    return this;
  }

  close(): void {
    this._edgeService.close();
  }

  edgeService(): EdgeService {
    return this._edgeService;
  }

  feature(name: string): FeatureStateHolder {
    return this._repository.feature(name).withContext(this);
  }

}
