import { FeatureListener, FeatureStateHolder } from './feature_state';
import { FeatureState, FeatureValueType } from './models/models';
import { ClientContext } from './client_context';
import { InternalFeatureRepository } from './internal_feature_repository';

export class InterceptorValueMatch {
  public value: string | undefined;

  constructor(value: string) {
    this.value = value;
  }
}

export interface FeatureStateValueInterceptor {
  matched(key: string): InterceptorValueMatch;
  repository(repo: InternalFeatureRepository): void;
}

export class FeatureStateBaseHolder implements FeatureStateHolder {
  protected featureState: FeatureState;
  protected _key: string;
  protected listeners: Array<FeatureListener> = [];
  protected _repo: InternalFeatureRepository;
  protected _ctx: ClientContext;

  constructor(repository: InternalFeatureRepository, key: string, existingHolder?: FeatureStateBaseHolder) {
    if (existingHolder !== null && existingHolder !== undefined) {
      this.listeners = existingHolder.listeners;
    }

    this._repo = repository;
    this._key = key;
  }

  public withContext(param: ClientContext): FeatureStateHolder {
    const fsh = this._copy();
    fsh._ctx = param;
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
    const val = this._getValue();
    return val !== undefined && val != null;
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
    return this.featureState?.type;
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
    const bh = new FeatureStateBaseHolder(this._repo, this._key); // don't copy listeners
    bh.setFeatureState(this.featureState);
    return bh;
  }

  private _getValue(type?: FeatureValueType): any | undefined {
    if (!this.isLocked()) {
      const intercept = this._repo.valueInterceptorMatched(this._key);

      if (intercept?.value) {
        return this._castType(type, intercept.value);
      }

      if (!this.featureState || (type != null && this.featureState.type !== type)) {
        return undefined;
      }

      if (this._ctx != null) {
        const matched = this._repo.apply(this.featureState.strategies, this._key, this.featureState.id, this._ctx);

        if (matched.matched) {
          return this._castType(type, matched.value);
        }
      }
    }


    return this.featureState?.value;
  }

  private _castType(type: FeatureValueType, value: any): any | undefined {
    if (value == null) {
      return undefined;
    }

    if (type === FeatureValueType.Boolean) {
      return typeof value === 'boolean' ? value : ('true' === value.toString());
    } else if (type === FeatureValueType.String) {
      return value.toString();
    } else if (type === FeatureValueType.Number) {
      if (typeof value === 'number') {
        return value;
      }
      if (value.includes('.')) {
        return parseFloat(value);
      }

      // tslint:disable-next-line:radix
      return parseInt(value);
    } else if (type === FeatureValueType.Json) {
      return value.toString();
    } else {
      return value.toString();
    }
  }
}
