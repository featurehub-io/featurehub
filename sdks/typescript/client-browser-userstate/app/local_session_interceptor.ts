
// this is a user centric repository, it overlays the featurehub repository
// and lets the user override non-locked features
// it stores data in the localSession

import { FeatureHubRepository, FeatureValueType, FeatureStateValueInterceptor, InterceptorValueMatch, fhLog } from 'featurehub-repository';

export class LocalSessionInterceptor implements FeatureStateValueInterceptor {
  private repo: FeatureHubRepository;
  private readonly _storage: Storage;
  private readonly _window: Window;
  private _alreadySetListener: boolean = false;

  constructor(win?: Window, storage?: Storage) {
    this._window = win ?? window;
    this._storage = storage || this._window.localStorage;
  }

  repository(repo: FeatureHubRepository): void {
    this.repo = repo;

    if (this._storage && !this._alreadySetListener) {
      this._alreadySetListener = true;
      this._window.addEventListener('storage', (e: StorageEvent) => LocalSessionInterceptor.storageChangedListener(e, this.repo));
    }
  }

  setUrl(url: string): void {
    if (this._storage) {
      this._storage.setItem('fh_url', url);
    }
  }

  public setFeatureValue(key: string, value: string): void {
    if (this._storage && this.repo) {
      const feature = this.repo.feature(key);

      if (feature.getType() === FeatureValueType.Boolean && value === undefined) {
        throw new Error('Cannot set boolean feature flag to null');
      }

      const nullCheck = this._storage[LocalSessionInterceptor._nullName(key)];
      const val = this._storage[LocalSessionInterceptor._valueName(key)];

      // we track if we are updating the _storage so the _storage changed
      // listener doesn't trigger too early. It can trigger after the second
      // change so it will if necessary force a state change.
      if (value === undefined) {
        this._storage.setItem(LocalSessionInterceptor._nullName(key), 'null');
        this._storage.removeItem(LocalSessionInterceptor._valueName(key));
        this._window.dispatchEvent(new StorageEvent('storage',
                                              {oldValue: val, newValue: value, key: LocalSessionInterceptor._valueName(key)}));
      } else {
        this._storage.setItem(LocalSessionInterceptor._valueName(key), value);
        this._storage.removeItem(LocalSessionInterceptor._nullName(key));
        this._window.dispatchEvent(new StorageEvent('storage',
                                              {oldValue: val, newValue: value, key: LocalSessionInterceptor._valueName(key)}) as StorageEvent);
      }
    }
  }

  matched(key: string): InterceptorValueMatch {
    if (this._storage && this.repo) {
      const nullCheck = this._storage[LocalSessionInterceptor._nullName(key)];

      if (nullCheck) {
        return new InterceptorValueMatch(undefined);
      }

      const val = this._storage[LocalSessionInterceptor._valueName(key)];

      if (val) {
        return new InterceptorValueMatch(val);
      }
    }

    return undefined;
  }

  private static _nullName(key: string): string  {
    return `fh_null_${key}`;
  }

  private static _valueName(key: string): string {
    return`fh_value_${key}`;
  }

  private static storageChangedListener(e: StorageEvent, repo: FeatureHubRepository) {
    let key: string = undefined;

    // ideal would be to have a post load event, find all the features
    // that have overridden states and track them as they change so
    // that we on a "clear" trigger the right listeners. This is MVP.
    if (e === undefined || e.key === undefined || e.key === null) {
      return;
    }

    if (repo === undefined) {
      fhLog.error('repo is undefined for the storage change listener.');
      return;
    }

    if (e.key.startsWith('fh_null_')) {
      key = e.key.substring('fh_null_'.length);
    } else if (e.key.startsWith('fh_value_')) {
      key = e.key.substring('fh_value_'.length);
    }

    if (key !== undefined && e.oldValue !== e.newValue) {
      // this will return a UserRepoHolder if we are actually overriding
      // and thus it is different from the underlying value
      const feature = repo.feature(key);
      if (feature && !feature.isLocked()) {
        feature.triggerListeners();
      }
    }
  }
}
