import {
  FeatureStateBaseHolder,
  FeatureStateBooleanHolder,
  FeatureStateJsonHolder,
  FeatureStateNumberHolder,
  FeatureStateStringHolder,
} from './feature_state_holders';

import { FeatureStateHolder } from './feature_state';

import { AnalyticsCollector } from './analytics';

import {
  FeatureState,
  FeatureValueType,
  SSEResultState,
  FeatureStateTypeTransformer
} from './models';
import { ClientContext } from './client_context';

export enum Readyness {
  NotReady = 'NotReady',
  Ready = 'Ready',
  Failed = 'Failed'
}

export interface ReadynessListener {
  (state: Readyness): void;
}

export interface PostLoadNewFeatureStateAvailableListener {
  (repo: ClientFeatureRepository): void;
}

export interface FeatureHubRepository {
  // determines if the repository is ready
  readyness: Readyness;

  // allows us to log an analytics event with this set of features
  logAnalyticsEvent(action: string, other?: Map<string, string>);

  // returns undefined if the feature does not exist
  hasFeature(key: string): FeatureStateHolder;

  // synonym for getFeatureState
  feature(key: string): FeatureStateHolder;

  // primary used to pass down the line in headers
  simpleFeatures(): Map<string, string|undefined>;
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
  private _clientContext = new ClientContext();

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

  public async broadcastReadynessState() {
    this.readynessListeners.forEach((l) => l(this.readynessState));
  }

  public addAnalyticCollector(collector: AnalyticsCollector): void {
    this.analyticsCollectors.push(collector);
  }

  public simpleFeatures(): Map<string, string|undefined> {
    const vals = new Map<string, string|undefined>();

    this.features.forEach((value, key) => {
      if (value.getKey()) { // only include value features
        const val = value.getFeatureState().value;
        vals.set(key, val === undefined ? val : val.toString());
      }
    });

    return vals;
  }

  public async logAnalyticsEvent(action: string, other?: Map<string, string>) {
    const featureStateAtCurrentTime = [];

    for (let fs of this.features.values()) {
      if (fs.isSet()) {
        featureStateAtCurrentTime.push(fs.copy());
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
      holder = new FeatureStateBaseHolder();
      this.features.set(key, holder);
    }

    return holder;
  }

  // deprecated
  public getFeatureState(key: string): FeatureStateHolder {
    return this.feature(key);
  }

  get clientContext(): ClientContext {
    return this._clientContext;
  }

  get catchAndReleaseMode(): boolean {
    return this._catchAndReleaseMode;
  }

  set catchAndReleaseMode(value: boolean) {
    this._catchAndReleaseMode = value;
  }

  public async release() {
    this._catchReleaseStates.forEach((fs) => this.featureUpdate(fs));
    this._catchReleaseStates.clear(); // remove all existing items
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
      switch (fs.type) {
        case FeatureValueType.Boolean:
          holder = new FeatureStateBooleanHolder(holder);
          break;
        case FeatureValueType.Json:
          holder = new FeatureStateJsonHolder(holder);
          break;
        case FeatureValueType.Number:
          holder = new FeatureStateNumberHolder(holder);
          break;
        case FeatureValueType.String:
          holder = new FeatureStateStringHolder(holder);
          break;
        default:
          return false;
      }

      if (holder !== undefined) {
        this.features.set(fs.key, holder);
      }
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
