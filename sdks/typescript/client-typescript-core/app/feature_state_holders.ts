import { FeatureListener, FeatureStateHolder } from './feature_state';
import { FeatureState, FeatureValueType } from './models/models';
import { FeatureHubRepository } from './client_feature_repository';
import { ClientContext } from './client_context';

export class InterceptorValueMatch {
  public value: string | undefined;

  constructor(value: string) {
    this.value = value;
  }
}

export interface FeatureStateValueInterceptor {
  matched(key: string): InterceptorValueMatch;
  repository(repo: FeatureHubRepository): void;
}

export class FeatureStateBaseHolder implements FeatureStateHolder {
  protected featureState: FeatureState;
  protected _key: string;
  protected listeners: Array<FeatureListener> = [];
  protected _repo: FeatureHubRepository;
  protected ctx: ClientContext;

  constructor(repository: FeatureHubRepository, key: string, existingHolder?: FeatureStateBaseHolder) {
    if (existingHolder !== null && existingHolder !== undefined) {
      this.listeners = existingHolder.listeners;
    }

    this._repo = repository;
    this._key = key;
  }

  public withContext(param: ClientContext): FeatureStateHolder {
    const fsh = this._copy();
    fsh.ctx = param;
    return fsh;
  }

  isEnabled(): boolean {
    return this.getBoolean() === true;
  }

  addListener(listener: FeatureListener): void {
    this.listeners.push(listener);
  }

  getBoolean(): boolean | undefined {
    return this._getValue(FeatureValueType.Boolean) as boolean | undefined;
  }

  getFlag(): boolean | undefined {
    return this.getBoolean();
  }

  getKey(): string | undefined {
    return this._key;
  }

  getNumber(): number | undefined {
    return this._getValue(FeatureValueType.Number) as number | undefined;
  }

  getRawJson(): string | undefined {
    return this._getValue(FeatureValueType.Json) as string | undefined;
  }

  getString(): string | undefined {
    return this._getValue(FeatureValueType.String) as string | undefined;
  }

  isSet(): boolean {
    return this._getValue();
  }

  getFeatureState(): FeatureState {
    return this.featureState;
  }

  /// returns true if the value changed
  setFeatureState(fs: FeatureState): boolean {
    const existingValue = this._getValue();
    const existingLocked = this.featureState?.l;

    this.featureState = fs;

    const changed = existingLocked !== this.featureState?.l || existingValue !== this._getValue();

    if (changed) {
      this.notifyListeners();
    }

    return changed;
  }

  copy(): FeatureStateHolder {
    return this._copy();
  }

  getType(): FeatureValueType | undefined {
    return undefined;
  }

  getVersion(): number | undefined {
    return this.featureState === undefined ? undefined : this.featureState.version;
  }

  isLocked(): boolean {
    return this.featureState === undefined ? undefined : this.featureState.l;
  }

  triggerListeners(feature: FeatureStateHolder): void {
    this.notifyListeners(feature);
  }

  protected async notifyListeners(feature?: FeatureStateHolder) {
    this.listeners.forEach((l) => {
      try {
        l(feature || this);
      } catch (e) {
        //
      } // don't care
    });
  }

  private _copy(): FeatureStateBaseHolder {
    return new FeatureStateBaseHolder(this._repo, this._key, this);
  }

  private _getValue(type?: FeatureValueType): any {
    if (!this.isLocked()) {
      const intercept = this._repo.valueInterceptorMatched(this._key);

      if (intercept?.value) {
        if (type === FeatureValueType.Boolean) {
          return 'true' === intercept.value;
        } else if (type === FeatureValueType.String) {
          return intercept.value;
        } else if (type === FeatureValueType.Number) {
          if (intercept.value.includes('.')) {
            return parseFloat(intercept.value);
          }

          // tslint:disable-next-line:radix
          return parseInt(intercept.value);
        } else if (type === FeatureValueType.Json) {
          return intercept.value;
        } else {
          return intercept.value;
        }

      }
    }

    if (this.featureState == null || (type != null && this.featureState.type !== type)) {
      return null;
    }

    return this.featureState?.value;
  }
}
