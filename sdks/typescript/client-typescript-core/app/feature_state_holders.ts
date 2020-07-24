import { FeatureListener, FeatureStateHolder } from './feature_state';
import { FeatureState } from './models/models';

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

  setFeatureState(fs: FeatureState): FeatureStateHolder {
    // always overridden
    return null;
  }

  copy(): FeatureStateHolder {
    return null;
  }

  protected notifyListeners() {
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

  constructor(existingHolder: FeatureStateBaseHolder) {
    super(existingHolder);
  }

  setFeatureState(fs: FeatureState): FeatureStateBooleanHolder {
    this.featureState = fs;
    const oldValue = this.value;
    this.value = fs.value !== undefined ? fs.value as unknown as boolean : undefined;
    if (oldValue !== this.value) {
      this.notifyListeners();
    }

    return this;
  }

  getBoolean(): boolean | undefined {
    return this.value;
  }

  isSet(): boolean {
    return this.value !== undefined;
  }

  copy(): FeatureStateHolder {
    return new FeatureStateBooleanHolder(null).setFeatureState(this.featureState);
  }
}

export class FeatureStateStringHolder extends FeatureStateBaseHolder {
  private value: string | undefined;

  constructor(existingHolder: FeatureStateBaseHolder) {
    super(existingHolder);
  }

  setFeatureState(fs: FeatureState): FeatureStateStringHolder {
    this.featureState = fs;
    const oldValue = this.value;
    this.value = fs.value !== undefined ? fs.value.toString() : undefined;
    if (oldValue !== this.value) {
      this.notifyListeners();
    }
    return this;
  }

  getString(): string | undefined {
    return this.value;
  }

  isSet(): boolean {
    return this.value !== undefined;
  }

  copy(): FeatureStateHolder {
    return new FeatureStateStringHolder(null).setFeatureState(this.featureState);
  }
}

export class FeatureStateNumberHolder extends FeatureStateBaseHolder {
  private value: number | undefined;

  constructor(existingHolder: FeatureStateBaseHolder) {
    super(existingHolder);
  }

  setFeatureState(fs: FeatureState): FeatureStateNumberHolder {
    this.featureState = fs;
    const oldValue = this.value;
    this.value = fs.value !== undefined ? fs.value as unknown as number : undefined;
    if (oldValue !== this.value) {
      this.notifyListeners();
    }
    return this;
  }

  getNumber(): number | undefined {
    return this.value;
  }

  isSet(): boolean {
    return this.value !== undefined;
  }

  copy(): FeatureStateHolder {
    return new FeatureStateNumberHolder(null).setFeatureState(this.featureState);
  }
}

export class FeatureStateJsonHolder extends FeatureStateBaseHolder {
  private value: string | undefined;

  constructor(existingHolder: FeatureStateBaseHolder) {
    super(existingHolder);
  }

  setFeatureState(fs: FeatureState): FeatureStateJsonHolder {
    this.featureState = fs;
    const oldValue = this.value;
    this.value = fs.value !== undefined ? fs.value.toString() : undefined;
    if (oldValue !== this.value) {
      this.notifyListeners();
    }

    return this;
  }

  getRawJson(): string | undefined {
    return this.value;
  }

  isSet(): boolean {
    return this.value !== undefined;
  }

  copy(): FeatureStateHolder {
    return new FeatureStateJsonHolder(null).setFeatureState(this.featureState);
  }
}
