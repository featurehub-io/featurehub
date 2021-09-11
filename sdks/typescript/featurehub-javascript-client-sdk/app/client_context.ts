import {
  StrategyAttributeCountryName,
  StrategyAttributeDeviceName,
  StrategyAttributePlatformName
} from './models';
import { FeatureStateHolder } from './feature_state';
import { FeatureHubRepository } from './featurehub_repository';

export interface ClientContext {
  userKey(value: string): ClientContext;
  sessionKey(value: string): ClientContext;
  country(value: StrategyAttributeCountryName): ClientContext;
  device(value: StrategyAttributeDeviceName): ClientContext;
  platform(value: StrategyAttributePlatformName): ClientContext;
  version(version: string): ClientContext;
  // eslint-disable-next-line camelcase
  attribute_value(key: string, value: string): ClientContext;
  // eslint-disable-next-line camelcase
  attribute_values(key: string, values: Array<string>): ClientContext;
  clear(): ClientContext;
  build(): Promise<ClientContext>;

  getAttr(key: string, defaultValue: string): string;
  getNumber(name: string): number | undefined;
  getString(name: string): string | undefined;
  getJson(name: string): any | undefined;
  getRawJson(name: string): string | undefined;
  getFlag(name: string): boolean | undefined;
  getBoolean(name: string): boolean | undefined;

  defaultPercentageKey(): string;

  feature(name: string): FeatureStateHolder;
  isEnabled(name: string): boolean;
  isSet(name: string): boolean;
  repository(): FeatureHubRepository;
  logAnalyticsEvent(action: string, other?: Map<string, string>, user?: string);

  close();
}
export interface ConfigChangedListener {
  (config: ClientContext);
}


