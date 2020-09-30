import { FeatureListener, FeatureStateHolder } from './feature_state';
import { FeatureState, FeatureValueType } from './models/models';
import { FeatureHubRepository } from './client_feature_repository';

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
  protected listeners: Array<FeatureListener> = [];
  protected matchers: Array<FeatureStateValueInterceptor> = [];

  constructor(existingHolder?: FeatureStateBaseHolder) {
    if (existingHolder !== null && existingHolder !== undefined) {
      this.matchers = existingHolder.matchers;
      this.listeners = existingHolder.listeners;
    }
  }

  addListener(listener: FeatureListener): void {
    this.listeners.push(listener);
  }

  addValueInterceptor(matcher: FeatureStateValueInterceptor): void {
    this.matchers.push(matcher);
  }

  getBoolean(): boolean | undefined {
    return undefined;
  }

  getKey(): string | undefined {
    return this.featureState === undefined ? undefined : this.featureState.key;
  }

  getNumber(): number | undefined {
    return undefined;
  }

  getRawJson(): string | undefined {
    return undefined;
  }

  getString(): string | undefined {
    return undefined;
  }

  isSet(): boolean {
    return false;
  }

  getFeatureState(): FeatureState {
    return this.featureState;
  }

  /// returns true if the value changed
  setFeatureState(fs: FeatureState): boolean {
    // always overridden
    return false;
  }

  copy(): FeatureStateHolder {
    return null;
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

  protected match(): InterceptorValueMatch | undefined {
    for (let match of this.matchers) {
      const val = match.matched(this.getKey());
      if (val) {
        return val;
      }
    }

    return undefined;
  }
}

export class FeatureStateBooleanHolder extends FeatureStateBaseHolder {
  private value: boolean | undefined;

  constructor(existingHolder: FeatureStateBaseHolder, featureState?: FeatureState) {
    super(existingHolder);

    if (featureState) {
      this.setFeatureState(featureState);
    }
  }

  setFeatureState(fs: FeatureState): boolean {
    const oldLocked = this.featureState?.l;
    this.featureState = fs;
    const oldValue = this.value;
    this.value = fs.value !== undefined ? fs.value as unknown as boolean : undefined;
    if (oldValue !== this.getBoolean() || (oldLocked !== fs.l && this.match())) {
      this.notifyListeners();
      return true;
    }

    return false;
  }

  getBoolean(): boolean | undefined {
    if (!this.isLocked()) {
      const matched = this.match();

      if (matched) {
        return matched.value === 'true';
      }
    }

    return this.value;
  }

  isSet(): boolean {
    return this.value !== undefined;
  }

  copy(): FeatureStateHolder {
    return new FeatureStateBooleanHolder(null, this.featureState);
  }

  getType(): FeatureValueType | undefined {
    return FeatureValueType.Boolean;
  }
}

export class FeatureStateStringHolder extends FeatureStateBaseHolder {
  private value: string | undefined;

  constructor(existingHolder: FeatureStateBaseHolder, featureState?: FeatureState) {
    super(existingHolder);

    if (featureState) {
      this.setFeatureState(featureState);
    }
  }

  setFeatureState(fs: FeatureState): boolean {
    const oldLocked = this.featureState?.l;
    this.featureState = fs;
    const oldValue = this.value;
    this.value = fs.value !== undefined ? fs.value.toString() : undefined;
    if (oldValue !== this.getString() || (oldLocked !== fs.l && this.match())) {
      this.notifyListeners();
      return true;
    }
    return false;
  }

  getString(): string | undefined {
    if (!this.isLocked()) {
      const matched = this.match();

      if (matched) {
        return matched.value;
      }
    }

    return this.value;
  }

  isSet(): boolean {
    return this.value !== undefined;
  }

  copy(): FeatureStateHolder {
    return new FeatureStateStringHolder(null, this.featureState);
  }

  getType(): FeatureValueType | undefined {
    return FeatureValueType.String;
  }
}

export class FeatureStateNumberHolder extends FeatureStateBaseHolder {
  private value: number | undefined;

  constructor(existingHolder: FeatureStateBaseHolder, featureState?: FeatureState) {
    super(existingHolder);

    if (featureState) {
      this.setFeatureState(featureState);
    }
  }

  setFeatureState(fs: FeatureState): boolean {
    const oldLocked = this.featureState?.l;
    this.featureState = fs;
    const oldValue = this.value;
    this.value = fs.value !== undefined ? fs.value as unknown as number : undefined;
    if (oldValue !== this.getNumber() || (oldLocked !== fs.l && this.match())) {
      this.notifyListeners();
      return true;
    }

    return false;
  }

  getNumber(): number | undefined {
    if (!this.isLocked()) {
      const matched = this.match();

      if (matched) {
        if (matched.value === undefined) {
          return undefined;
        }

        if (matched.value.includes('.')) {
          return parseFloat(matched.value);
        }

        // tslint:disable-next-line:radix
        return parseInt(matched.value);
      }
    }

    return this.value;
  }

  isSet(): boolean {
    return this.value !== undefined;
  }

  copy(): FeatureStateHolder {
    return new FeatureStateNumberHolder(null, this.featureState);
  }

  getType(): FeatureValueType | undefined {
    return FeatureValueType.Number;
  }
}

export class FeatureStateJsonHolder extends FeatureStateBaseHolder {
  private value: string | undefined;

  constructor(existingHolder: FeatureStateBaseHolder, featureState?: FeatureState) {
    super(existingHolder);

    if (featureState) {
      this.setFeatureState(featureState);
    }
  }

  setFeatureState(fs: FeatureState): boolean {
    const oldLocked = this.featureState?.l;
    this.featureState = fs;
    const oldValue = this.value;
    this.value = fs.value !== undefined ? fs.value.toString() : undefined;
    if (oldValue !== this.getRawJson() || (oldLocked !== fs.l && this.match()) ) {
      this.notifyListeners();
      return true;
    }

    return false;
  }

  getRawJson(): string | undefined {
    if (!this.isLocked()) {

      const matched = this.match();

      if (matched) {
        return matched.value;
      }
    }

    return this.value;
  }

  isSet(): boolean {
    return this.value !== undefined;
  }

  copy(): FeatureStateHolder {
    return new FeatureStateJsonHolder(null, this.featureState);
  }

  getType(): FeatureValueType | undefined {
    return FeatureValueType.Json;
  }
}
