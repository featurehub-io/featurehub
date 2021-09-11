import { ClientContext } from './client_context';
import { FeatureStateHolder } from './feature_state';
import { FeatureStateValueInterceptor } from './interceptors';
import { AnalyticsCollector } from './analytics';
import { InternalFeatureRepository } from './internal_feature_repository';

export enum Readyness {
  NotReady = 'NotReady',
  Ready = 'Ready',
  Failed = 'Failed'
}

export interface ReadynessListener {
  (state: Readyness): void;
}

export interface PostLoadNewFeatureStateAvailableListener {
  (repo: InternalFeatureRepository): void;
}

export interface FeatureHubRepository {
  // determines if the repository is ready
  readyness: Readyness;
  catchAndReleaseMode: boolean;

  // allows us to log an analytics event with this set of features
  logAnalyticsEvent(action: string, other?: Map<string, string>, ctx?: ClientContext);

  // returns undefined if the feature does not exist
  hasFeature(key: string): undefined | FeatureStateHolder;

  // synonym for getFeatureState
  feature(key: string): FeatureStateHolder;

  // deprecated
  getFeatureState(key: string): FeatureStateHolder;

  // release changes
  release(disableCatchAndRelease?: boolean): Promise<void>;

  // primary used to pass down the line in headers
  simpleFeatures(): Map<string, string | undefined>;

  getFlag(key: string): boolean | undefined;

  getString(key: string): string | undefined;

  getJson(key: string): string | undefined;

  getNumber(key: string): number | undefined;

  isSet(key: string): boolean;

  addValueInterceptor(interceptor: FeatureStateValueInterceptor): void;

  addReadynessListener(listener: ReadynessListener): void;

  addAnalyticCollector(collector: AnalyticsCollector): void;

  addPostLoadNewFeatureStateAvailableListener(listener: PostLoadNewFeatureStateAvailableListener);

}
