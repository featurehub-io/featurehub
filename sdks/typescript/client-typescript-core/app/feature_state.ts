import { FeatureValueType } from './models/models';

export interface FeatureListener {
  (featureChanged: FeatureStateHolder): void;
}

export interface FeatureStateHolder {
  getKey(): string | undefined;

  getString(): string | undefined;

  getBoolean(): boolean | undefined;

  getNumber(): number | undefined;

  getRawJson(): string | undefined;

  isSet(): boolean;

  isLocked(): boolean | undefined;

  addListener(listener: FeatureListener): void;

  getVersion(): number | undefined;

  getType(): FeatureValueType | undefined;
}
