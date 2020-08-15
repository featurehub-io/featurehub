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

export class ClientFeatureRepository {

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
              this.checkForInvalidFeatures();
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

  public broadcastReadynessState() {
    this.readynessListeners.forEach((l) => l(this.readynessState));
  }

  public addAnalyticCollector(collector: AnalyticsCollector): void {
    this.analyticsCollectors.push(collector);
  }

  public logAnalyticsEvent(action: string, other?: Map<string, string>): void {
    const featureStateAtCurrentTime = [];

    for (let fs of this.features.values()) {
      if (fs.isSet()) {
        featureStateAtCurrentTime.push(fs.copy());
      }
    }

    this.analyticsCollectors.forEach((ac) => ac.logEvent(action, other, featureStateAtCurrentTime));
  }

  public getFeatureState(key: string): FeatureStateHolder {
    let holder = this.features.get(key);

    if (holder === undefined) {
      holder = new FeatureStateBaseHolder();
      this.features.set(key, holder);
    }

    return holder;
  }

  get catchAndReleaseMode(): boolean {
    return this._catchAndReleaseMode;
  }

  set catchAndReleaseMode(value: boolean) {
    this._catchAndReleaseMode = value;
  }

  public release(): void {
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

  private triggerNewStateAvailable() {
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
      return;
    }

    let holder = this.features.get(fs.key);
    if (holder === undefined || holder.getKey() === undefined) {
      switch (fs.type) {
        case FeatureValueType.BOOLEAN:
          holder = new FeatureStateBooleanHolder(holder);
          break;
        case FeatureValueType.JSON:
          holder = new FeatureStateJsonHolder(holder);
          break;
        case FeatureValueType.NUMBER:
          holder = new FeatureStateNumberHolder(holder);
          break;
        case FeatureValueType.STRING:
          holder = new FeatureStateStringHolder(holder);
          break;
        default:
          break;
      }

      if (holder !== undefined) {
        this.features.set(fs.key, holder);
      }
    } else if (fs.version <= holder.getFeatureState().version) {
      return false;
    }

    if (holder !== undefined) {
      holder.setFeatureState(fs);
    }

    return true;
  }

  private checkForInvalidFeatures(): void {

    /*
        String invalidKeys = features.values().stream().filter(v ->
        v.getKey() == null).map(FeatureStateHolder::getKey).collect(Collectors.joining(", "));
        if (invalidKeys.length() > 0) {
          log.error("FeatureHub error: application is requesting use of invalid keys: {}", invalidKeys);
        }
     */
  }

  private deleteFeature(featureState: FeatureState) {
    featureState.value = undefined;
    this.featureUpdate(featureState);
  }
}
