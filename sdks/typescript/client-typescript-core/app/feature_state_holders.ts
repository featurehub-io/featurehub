import { FeatureListener, FeatureStateHolder } from './feature_state';
import { FeatureState, FeatureValueType } from './models/models';

export class FeatureStateBaseHolder implements FeatureStateHolder {
  protected featureState: FeatureState;
  protected listeners: Array<FeatureListener> = [];

  constructor(existingHolder?: FeatureStateBaseHolder) {
    if (existingHolder !== null && existingHolder !== undefined) {
      this.listeners = existingHolder.listeners;
    }
  }

  addListener(listener: FeatureListener): void {
    this.listeners.push(listener);
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

  protected async notifyListeners() {
    this.listeners.forEach((l) => {
      try {
        l(this);
      } catch (e) {
        //
      } // don't care
    });
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
    this.featureState = fs;
    const oldValue = this.value;
    this.value = fs.value !== undefined ? fs.value as unknown as boolean : undefined;
    if (oldValue !== this.value) {
      this.notifyListeners();
      return true;
    }

    return false;
  }

  getBoolean(): boolean | undefined {
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
    this.featureState = fs;
    const oldValue = this.value;
    this.value = fs.value !== undefined ? fs.value.toString() : undefined;
    if (oldValue !== this.value) {
      this.notifyListeners();
      return true;
    }
    return false;
  }

  getString(): string | undefined {
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
    this.featureState = fs;
    const oldValue = this.value;
    this.value = fs.value !== undefined ? fs.value as unknown as number : undefined;
    if (oldValue !== this.value) {
      this.notifyListeners();
      return true;
    }

    return false;
  }

  getNumber(): number | undefined {
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
    this.featureState = fs;
    const oldValue = this.value;
    this.value = fs.value !== undefined ? fs.value.toString() : undefined;
    if (oldValue !== this.value) {
      this.notifyListeners();
      return true;
    }

    return false;
  }

  getRawJson(): string | undefined {
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
