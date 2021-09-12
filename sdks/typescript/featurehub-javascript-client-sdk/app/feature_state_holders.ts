import { FeatureListener, FeatureStateHolder } from './feature_state';
import { FeatureState, FeatureValueType } from './models';
import { ClientContext } from './client_context';
import { InternalFeatureRepository } from './internal_feature_repository';

export class FeatureStateBaseHolder implements FeatureStateHolder {
  protected internalFeatureState: FeatureState;
  protected _key: string;
  protected listeners: Array<FeatureListener> = [];
  protected _repo: InternalFeatureRepository;
  protected _ctx: ClientContext;
  protected parentHolder: FeatureStateBaseHolder;

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

  public isEnabled(): boolean {
    return this.getBoolean() === true;
  }

  public addListener(listener: FeatureListener): void {
    if (this._ctx !== undefined) {
      this.listeners.push(() => listener(this));
    } else {
      this.listeners.push(listener);
    }
  }

  public getBoolean(): boolean | undefined {
    return this._getValue(FeatureValueType.Boolean) as boolean | undefined;
  }

  public getFlag(): boolean | undefined {
    return this.getBoolean();
  }

  public getKey(): string | undefined {
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
    return this.featureState();
  }

  /// returns true if the value changed, _only_ the repository should call this
  /// as it is dereferenced via the parentHolder
  setFeatureState(fs: FeatureState): boolean {
    const existingValue = this._getValue();
    const existingLocked = this.featureState()?.l;

    this.internalFeatureState = fs;

    const changed = existingLocked !== this.featureState()?.l || existingValue !== this._getValue();

    if (changed) {
      this.notifyListeners();
    }

    return changed;
  }

  copy(): FeatureStateHolder {
    return this._copy();
  }

  // we need the internal feature state set to be consistent
  analyticsCopy(): FeatureStateBaseHolder {
    const c = this._copy();
    c.internalFeatureState = this.internalFeatureState;
    return c;
  }

  getType(): FeatureValueType | undefined {
    return this.featureState()?.type;
  }

  getVersion(): number | undefined {
    return this.featureState() === undefined ? undefined : this.featureState().version;
  }

  isLocked(): boolean {
    return this.featureState() === undefined ? undefined : this.featureState().l;
  }

  triggerListeners(feature: FeatureStateHolder): void {
    this.notifyListeners(feature);
  }

  protected notifyListeners(feature?: FeatureStateHolder): void {
    this.listeners.forEach((l) => {
      try {
        l(feature || this);
      } catch (e) {
        //
      } // don't care
    });
  }

  private _copy(): FeatureStateBaseHolder {
    const bh = new FeatureStateBaseHolder(this._repo, this._key, this);
    bh.parentHolder = this;
    return bh;
  }

  private featureState(): FeatureState {
    if (this.internalFeatureState !== undefined) {
      return this.internalFeatureState;
    }

    if (this.parentHolder !== undefined) {
      return this.parentHolder.featureState();
    }

    return this.internalFeatureState;
  }

  private _getValue(type?: FeatureValueType): any | undefined {
    if (!this.isLocked()) {
      const intercept = this._repo.valueInterceptorMatched(this._key);

      if (intercept?.value) {
        return this._castType(type, intercept.value);
      }
    }

    const featureState = this.featureState();
    if (!featureState || (type != null && featureState.type !== type)) {
      return undefined;
    }

    if (this._ctx != null) {
      const matched = this._repo.apply(featureState.strategies, this._key, featureState.id, this._ctx);

      if (matched.matched) {
        return this._castType(type, matched.value);
      }
    }

    return featureState?.value;
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
