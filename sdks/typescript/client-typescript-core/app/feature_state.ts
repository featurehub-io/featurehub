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

  addListener(listener: FeatureListener): void;
}
