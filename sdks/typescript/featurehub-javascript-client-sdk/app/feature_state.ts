import { FeatureValueType } from './models';
import { ClientContext } from './client_context';

// these two depend on each other

export interface FeatureListener {
  // eslint-disable-next-line no-use-before-define
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

  isEnabled(): boolean;

  addListener(listener: FeatureListener): void;

  // this is intended for override repositories (such as the UserFeatureRepository)
  // to force the listeners to trigger if they detect an actual state change in their layer
  // it passes in the feature state holder to notify with
  triggerListeners(feature?: FeatureStateHolder): void;

  getVersion(): number | undefined;

  getType(): FeatureValueType | undefined;

  withContext(param: ClientContext): FeatureStateHolder;
}
