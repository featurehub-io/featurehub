import { FeatureValueType } from './models/models';
import { FeatureStateValueInterceptor } from './feature_state_holders';

export interface FeatureListener {
  (featureChanged: FeatureStateHolder): void;
}

export interface FeatureStateHolder {
  getKey(): string | undefined;

  getString(): string | undefined;

  getBoolean(): boolean | undefined;

  getFlag(): boolean | undefined;

  getNumber(): number | undefined;

  getRawJson(): string | undefined;

  isSet(): boolean;

  isLocked(): boolean | undefined;

  addListener(listener: FeatureListener): void;

  addValueInterceptor(matcher: FeatureStateValueInterceptor): void;

  // this is intended for override repositories (such as the UserFeatureRepository)
  // to force the listeners to trigger if they detect an actual state change in their layer
  // it passes in the feature state holder to notify with
  triggerListeners(feature?: FeatureStateHolder): void;

  getVersion(): number | undefined;

  getType(): FeatureValueType | undefined;
}
