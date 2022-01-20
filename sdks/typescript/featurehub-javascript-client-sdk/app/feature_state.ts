import { FeatureValueType } from './models';
import { ClientContext } from './client_context';

// these two depend on each other

export interface FeatureListener {
  // eslint-disable-next-line no-use-before-define
  (featureChanged: FeatureStateHolder): void;
}

export interface FeatureStateHolder {
  getKey(): string | undefined;

  get key(): string | undefined;

  getString(): string | undefined;

  get str(): string | undefined;

  getBoolean(): boolean | undefined;

  getFlag(): boolean | undefined;

  get flag(): boolean | undefined;

  /**
   * A number (or undefined if no value exists for it). Only for a number feature.
   */
  getNumber(): number | undefined;

  get num(): number | undefined;

  /**
   * getRawJson(): The raw json value of the data
   */
  getRawJson(): string | undefined;

  get rawJson(): string | undefined;

  /**
   * isSet: does this feature value a value
   */
  isSet(): boolean;

  /**
   * exists: does this feature actually exist, have we ever received state for it. If it was asked
   * for before it we received state for it and it still has no state, or it was deleted, this will be
   * false, otherwise it will be true.
   */
  get exists(): boolean;

  isLocked(): boolean | undefined;

  get locked(): boolean | undefined;

  isEnabled(): boolean;

  get enabled(): boolean;

  addListener(listener: FeatureListener): void;

  // this is intended for override repositories (such as the UserFeatureRepository)
  // to force the listeners to trigger if they detect an actual state change in their layer
  // it passes in the feature state holder to notify with
  triggerListeners(feature?: FeatureStateHolder): void;

  getVersion(): number | undefined;

  get version(): number | undefined;

  getType(): FeatureValueType | undefined;

  get type(): FeatureValueType | undefined;

  withContext(param: ClientContext): FeatureStateHolder;
}
