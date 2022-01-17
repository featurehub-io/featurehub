import { FeatureStateBaseHolder } from './feature_state_holders';
import { FeatureStateValueInterceptor, InterceptorValueMatch } from './interceptors';

import { FeatureStateHolder } from './feature_state';

import { AnalyticsCollector } from './analytics';
// leave this here, prevents circular deps
import { FeatureState, FeatureValueType, RolloutStrategy, SSEResultState } from './models';
import { ClientContext } from './client_context';
import { Applied, ApplyFeature } from './strategy_matcher';
import { InternalFeatureRepository } from './internal_feature_repository';
import { fhLog } from './feature_hub_config';
import { PostLoadNewFeatureStateAvailableListener, Readyness, ReadynessListener } from './featurehub_repository';

export class ClientFeatureRepository implements InternalFeatureRepository {
  private hasReceivedInitialState: boolean;
  // indexed by key as that what the user cares about
  private features = new Map<string, FeatureStateBaseHolder>();
  private analyticsCollectors = new Array<AnalyticsCollector>();
  private readynessState: Readyness = Readyness.NotReady;
  private readynessListeners: Array<ReadynessListener> = [];
  private _catchAndReleaseMode = false;
  // indexed by id
  private _catchReleaseStates = new Map<string, FeatureState>();
  private _newFeatureStateAvailableListeners: Array<PostLoadNewFeatureStateAvailableListener> = [];
  private _matchers: Array<FeatureStateValueInterceptor> = [];
  private readonly _applyFeature: ApplyFeature;

  constructor(applyFeature?: ApplyFeature) {
    this._applyFeature = applyFeature || new ApplyFeature();
  }

  public apply(strategies: Array<RolloutStrategy>, key: string, featureValueId: string,
    context: ClientContext): Applied {
    return this._applyFeature.apply(strategies, key, featureValueId, context);
  }

  public get readyness(): Readyness {
    return this.readynessState;
  }

  public notify(state: SSEResultState, data: any) {
    if (state !== null && state !== undefined) {
      switch (state) {
        case SSEResultState.Ack: // do nothing, expect state shortly
        case SSEResultState.Bye: // do nothing, we expect a reconnection shortly
          break;
        case SSEResultState.DeleteFeature:
          this.deleteFeature(data);
          break;
        case SSEResultState.Failure:
          this.readynessState = Readyness.Failed;
          if (!this._catchAndReleaseMode) {
            this.broadcastReadynessState();
          }
          break;
        case SSEResultState.Feature: {
          const fs = data instanceof FeatureState ? data : new FeatureState(data);

          if (this._catchAndReleaseMode) {
            this._catchUpdatedFeatures([fs]);
          } else {
            if (this.featureUpdate(fs)) {
              this.triggerNewStateAvailable();
            }
          }
        }
          break;
        case SSEResultState.Features: {
          const features = (data as []).map((f : any) => f instanceof FeatureState ? f : new FeatureState(f));
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
        }
          break;
        default:
          break;
      }
    }
  }

  public addValueInterceptor(matcher: FeatureStateValueInterceptor): void {
    this._matchers.push(matcher);

    matcher.repository(this);
  }

  public valueInterceptorMatched(key: string): InterceptorValueMatch {
    for (const matcher of this._matchers) {
      const m = matcher.matched(key);
      if (m?.value) {
        return m;
      }
    }

    return null;
  }

  public addPostLoadNewFeatureStateAvailableListener(listener: PostLoadNewFeatureStateAvailableListener): void {
    this._newFeatureStateAvailableListeners.push(listener);

    if (this._catchReleaseStates.size > 0) {
      listener(this);
    }
  }

  public addReadynessListener(listener: ReadynessListener): void {
    this.readynessListeners.push(listener);

    // always let them know what it is in case its already ready
    listener(this.readynessState);
  }

  notReady(): void {
    this.readynessState = Readyness.NotReady;
    this.broadcastReadynessState();
  }

  public broadcastReadynessState(): void {
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

  public logAnalyticsEvent(action: string, other?: Map<string, string>, ctx?: ClientContext): void {
    const featureStateAtCurrentTime = [];

    for (const fs of this.features.values()) {
      if (fs.isSet()) {
        const fsVal: FeatureStateBaseHolder = ctx == null ? fs : fs.withContext(ctx) as FeatureStateBaseHolder;
        featureStateAtCurrentTime.push(fsVal.analyticsCopy());
      }
    }

    this.analyticsCollectors.forEach((ac) => ac.logEvent(action, other, featureStateAtCurrentTime));
  }

  public hasFeature(key: string): undefined | FeatureStateHolder {
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

  // eslint-disable-next-line require-await
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
        if (!existingFeature || (existingFeature.getKey()
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

  private triggerNewStateAvailable(): void {
    if (this.hasReceivedInitialState && this._newFeatureStateAvailableListeners.length > 0) {
      if (!this._catchAndReleaseMode || (this._catchReleaseStates.size > 0)) {
        this._newFeatureStateAvailableListeners.forEach((l) => {
          try {
            l(this);
          } catch (e) {
            fhLog.log('failed', e);
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
    if (holder === undefined) {
      const newFeature = new FeatureStateBaseHolder(this, fs.key, holder);

      this.features.set(fs.key, newFeature);

      holder = newFeature;
    } else if (holder.getFeatureState() !== undefined) {
      if (fs.version < holder.getFeatureState().version) {
        return false;
      } else if (fs.version === holder.getFeatureState().version && fs.value === holder.getFeatureState().value) {
        return false;
      }
    }

    return holder.setFeatureState(fs);
  }

  private deleteFeature(featureState: FeatureState) {
    featureState.value = undefined;

    const holder = this.features.get(featureState.key);

    if (holder) {
      holder.setFeatureState(featureState);
    }
  }
}
