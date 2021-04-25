import {
  FeatureStateBaseHolder,
  FeatureStateValueInterceptor, InterceptorValueMatch,
} from './feature_state_holders';

import { FeatureStateHolder } from './feature_state';

import { AnalyticsCollector } from './analytics';

import { FeatureState, FeatureStateTypeTransformer, FeatureValueType, SSEResultState } from './models';
import { ClientContext } from './client_context';

export enum Readyness {
  NotReady = 'NotReady',
  Ready = 'Ready',
  Failed = 'Failed'
}

export interface ReadynessListener {
  (state: Readyness): void;
}

export class ClientFeatureRepository implements FeatureHubRepository {
  private hasReceivedInitialState: boolean;
  // indexed by key as that what the user cares about
  private features = new Map<string, FeatureStateBaseHolder>();
  private analyticsCollectors = new Array<AnalyticsCollector>();
  private readynessState: Readyness = Readyness.NotReady;
  private readynessListeners: Array<ReadynessListener> = [];
  private _catchAndReleaseMode: boolean = false;
  // indexed by id
  private _catchReleaseStates = new Map<string, FeatureState>();
  private _newFeatureStateAvailableListeners: Array<PostLoadNewFeatureStateAvailableListener> = [];
  private _matchers: Array<FeatureStateValueInterceptor> = [];

  public get readyness(): Readyness {
    return this.readynessState;
  }

  public notify(state: SSEResultState, data: any): void {
    if (state !== null && state !== undefined) {
      switch (state) {
        case SSEResultState.Ack:
          break;
        case SSEResultState.Bye:
          this.readynessState = Readyness.NotReady;
          if (!this._catchAndReleaseMode) {
            this.broadcastReadynessState();
          }
          break;
        case SSEResultState.DeleteFeature:
          this.deleteFeature(FeatureStateTypeTransformer.fromJson(data));
          break;
        case SSEResultState.Failure:
          this.readynessState = Readyness.Failed;
          if (!this._catchAndReleaseMode) {
            this.broadcastReadynessState();
          }
          break;
        case SSEResultState.Feature:
          const fs = FeatureStateTypeTransformer.fromJson(data);

          if (this._catchAndReleaseMode) {
            this._catchUpdatedFeatures([fs]);
          } else {
            if (this.featureUpdate(fs)) {
              this.triggerNewStateAvailable();
            }
          }

          break;
        case SSEResultState.Features:
          const features = (data instanceof Array) ? (data as Array<FeatureState>) :
            (data as []).map((f) => FeatureStateTypeTransformer.fromJson(f));
          if (this.hasReceivedInitialState && this._catchAndReleaseMode) {

            this._catchUpdatedFeatures(features);
          } else {
            let updated = false;
            features.forEach((f) => updated = this.featureUpdate(f) || updated);
            this.readynessState = Readyness.Ready;
            if (!this.hasReceivedInitialState) {
              this.hasReceivedInitialState = true;
              this.broadcastReadynessState();
            } else if (updated) {
              this.triggerNewStateAvailable();
            }
          }
          break;
        default:
          break;
      }
    }
  }

  public addValueInterceptor(matcher: FeatureStateValueInterceptor) {
    this._matchers.push(matcher);

    matcher.repository(this);
  }

  public valueInterceptorMatched(key: string): InterceptorValueMatch {
    for (let matcher of this._matchers) {
      const m = matcher.matched(key);
      if (m.value) {
        return m;
      }
    }

    return null;
  }

  public addPostLoadNewFeatureStateAvailableListener(listener: PostLoadNewFeatureStateAvailableListener) {
    this._newFeatureStateAvailableListeners.push(listener);

    if (this._catchReleaseStates.size > 0) {
      listener(this);
    }
  }

  public addReadynessListener(listener: ReadynessListener) {
    this.readynessListeners.push(listener);

    // always let them know what it is in case its already ready
    listener(this.readynessState);
  }

  notReady(): void {
    this.readynessState = Readyness.NotReady;
    this.broadcastReadynessState();
  }

  public async broadcastReadynessState() {
    this.readynessListeners.forEach((l) => l(this.readynessState));
  }

  public addAnalyticCollector(collector: AnalyticsCollector): void {
    this.analyticsCollectors.push(collector);
  }

  public simpleFeatures(): Map<string, string | undefined> {
    const vals = new Map<string, string | undefined>();

    this.features.forEach((value, key) => {
      if (value.getKey()) { // only include value features
        let val: any;
        switch (value.getType()) {// we need to pick up any overrides
          case FeatureValueType.Boolean:
            val = value.getBoolean() ? 'true' : 'false';
            break;
          case FeatureValueType.String:
            val = value.getString();
            break;
          case FeatureValueType.Number:
            val = value.getNumber();
            break;
          case FeatureValueType.Json:
            val = value.getRawJson();
            break;
          default:
            val = undefined;
        }
        vals.set(key, val === undefined ? val : val.toString());
      }
    });

    return vals;
  }

  public async logAnalyticsEvent(action: string, other?: Map<string, string>, ctx?: ClientContext) {
    const featureStateAtCurrentTime = [];

    for (let fs of this.features.values()) {
      if (fs.isSet()) {
        featureStateAtCurrentTime.push(ctx == null ? fs.copy() : fs.withContext(ctx));
      }
    }

    this.analyticsCollectors.forEach((ac) => ac.logEvent(action, other, featureStateAtCurrentTime));
  }

  public hasFeature(key: string): FeatureStateHolder {
    return this.features.get(key);
  }

  public feature(key: string): FeatureStateHolder {
    let holder = this.features.get(key);

    if (holder === undefined) {
      holder = new FeatureStateBaseHolder(this, key);
      this.features.set(key, holder);
    }

    return holder;
  }

  // deprecated
  public getFeatureState(key: string): FeatureStateHolder {
    return this.feature(key);
  }

  get catchAndReleaseMode(): boolean {
    return this._catchAndReleaseMode;
  }

  set catchAndReleaseMode(value: boolean) {
    if (this._catchAndReleaseMode !== value && value === false) {
      this.release(true);
    }
    this._catchAndReleaseMode = value;
  }

  public async release(disableCatchAndRelease?: boolean): Promise<void> {
    while (this._catchReleaseStates.size > 0) {
      const states = [...this._catchReleaseStates.values()];
      this._catchReleaseStates.clear(); // remove all existing items
      states.forEach((fs) => this.featureUpdate(fs));
    }

    if (disableCatchAndRelease === true) {
      this._catchAndReleaseMode = false;
    }
  }

  public getFlag(key: string): boolean | undefined {
    return this.feature(key).getFlag();
  }

  public getString(key: string): string | undefined {
    return this.feature(key).getString();
  }

  public getJson(key: string): string | undefined {
    return this.feature(key).getRawJson();
  }

  public getNumber(key: string): number | undefined {
    return this.feature(key).getNumber();
  }

  public isSet(key: string): boolean {
    return this.feature(key).isSet();
  }

  private _catchUpdatedFeatures(features: FeatureState[]) {
    let updatedValues = false;
    if (features && features.length > 0) {
      features.forEach((f) => {
        const existingFeature = this.features.get(f.key);
        if (existingFeature === null || (existingFeature.getKey()
          && f.version > existingFeature.getFeatureState().version)) {
          const fs = this._catchReleaseStates.get(f.id);
          if (fs == null) {
            this._catchReleaseStates.set(f.id, f);
            updatedValues = true;
          } else {
            // check it is newer
            if (fs.version === undefined || (f.version !== undefined && f.version > fs.version)) {
              this._catchReleaseStates.set(f.id, f);
              updatedValues = true;
            }
          }
        }
      });
    }
    if (updatedValues) {
      this.triggerNewStateAvailable();
    }
  }

  private async triggerNewStateAvailable() {
    if (this.hasReceivedInitialState && this._newFeatureStateAvailableListeners.length > 0) {
      if (!this._catchAndReleaseMode || (this._catchReleaseStates.size > 0)) {
        this._newFeatureStateAvailableListeners.forEach((l) => {
          try {
            l(this);
          } catch (e) {
            console.log('failed', e);
          }
        });
      }
    } else {
      // console.log('new data, no listeners');
    }
  }

  private featureUpdate(fs: FeatureState): boolean {
    if (fs === undefined || fs.key === undefined) {
      return false;
    }

    let holder = this.features.get(fs.key);
    if (holder === undefined || holder.getKey() === undefined) {
      const newFeature = new FeatureStateBaseHolder(this, fs.key);
      newFeature.setFeatureState(fs);

      this.features.set(fs.key, newFeature);

      holder = newFeature;
    } else if (fs.version < holder.getFeatureState().version) {
      return false;
    } else if (fs.version === holder.getFeatureState().version && fs.value === holder.getFeatureState().value) {
      return false;
    }

    return holder.setFeatureState(fs);
  }

  private deleteFeature(featureState: FeatureState) {
    featureState.value = undefined;

    let holder = this.features.get(featureState.key);

    if (holder) {
      holder.setFeatureState(featureState);
    }
  }
}

export interface PostLoadNewFeatureStateAvailableListener {
  (repo: ClientFeatureRepository): void;
}

export interface FeatureHubRepository {
  // determines if the repository is ready
  readyness: Readyness;

  // allows us to log an analytics event with this set of features
  logAnalyticsEvent(action: string, other?: Map<string, string>, ctx?: ClientContext);

  // returns undefined if the feature does not exist
  hasFeature(key: string): FeatureStateHolder;

  // synonym for getFeatureState
  feature(key: string): FeatureStateHolder;

  // primary used to pass down the line in headers
  simpleFeatures(): Map<string, string | undefined>;

  getFlag(key: string): boolean | undefined;

  getString(key: string): string | undefined;

  getJson(key: string): string | undefined;

  getNumber(key: string): number | undefined;

  isSet(key: string): boolean;

  notReady(): void;

  notify(state: SSEResultState, data: any): void;

  addValueInterceptor(interceptor: FeatureStateValueInterceptor);

  valueInterceptorMatched(key: string): InterceptorValueMatch;
}
