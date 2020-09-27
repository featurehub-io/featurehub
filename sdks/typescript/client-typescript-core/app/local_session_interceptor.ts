
// this is a user centric repository, it overlays the featurehub repository
// and lets the user override non-locked features
// it stores data in the localSession

import { FeatureHubRepository } from './client_feature_repository';
import { FeatureValueType } from './models/models';
import { FeatureStateValueInterceptor, InterceptorValueMatch } from './feature_state_holders';

export class LocalSessionInterceptor implements FeatureStateValueInterceptor {
  private repo: FeatureHubRepository;
  private storage: Storage;
  private _alreadySetListener: boolean = false;

  constructor() {
    this.storage = window.localStorage;
  }

  repository(repo: FeatureHubRepository): void {

    this.repo = repo;

    if (this.storage && !this._alreadySetListener) {
      this._alreadySetListener = true;
      window.addEventListener('storage', (e: StorageEvent) => this.storageChangedListener(e, this.repo));
    }
  }

  public setFeatureValue(key: string, value: string): void {
    if (this.storage && this.repo) {
      const feature = this.repo.feature(key);

      if (feature.getType() === FeatureValueType.Boolean && value === undefined) {
        throw new Error('Cannot set boolean feature flag to null');
      }

      const nullCheck = this.storage[this._nullName(key)];
      const val = this.storage[this._valueName(key)];

      // we track if we are updating the storage so the storage changed
      // listener doesn't trigger too early. It can trigger after the second
      // change so it will if necessary force a state change.
      if (value === undefined) {
        this.storage.setItem(this._nullName(key), 'null');
        this.storage.removeItem(this._valueName(key));
        console.log('dispatch',
                    window.dispatchEvent(new StorageEvent('storage',
                                              {oldValue: nullCheck, newValue: 'null', key: this._nullName(key)})));
        window.dispatchEvent(new StorageEvent('storage',
                                              {oldValue: val, newValue: value, key: this._valueName(key)}));
      } else {
        this.storage.setItem(this._valueName(key), value);
        this.storage.removeItem(this._nullName(key));
        console.log('dispatch2',
                    window.dispatchEvent(new StorageEvent('storage',
                      // tslint:disable-next-line:max-line-length
                                                          {oldValue: nullCheck, newValue: undefined, key: this._nullName(key)})));
        window.dispatchEvent(new StorageEvent('storage',
                                              {oldValue: val, newValue: value, key: this._valueName(key)}));
      }
    }
  }

  matched(key: string): InterceptorValueMatch {
    if (this.storage && this.repo) {
      const nullCheck = this.storage[this._nullName(key)];

      if (nullCheck) {
        return new InterceptorValueMatch(undefined);
      }

      const val = this.storage[this._valueName(key)];

      if (val) {
        return new InterceptorValueMatch(val);
      }
    }

    return undefined;
  }

  private _nullName(key: string): string  {
    return `fh_null_${key}`;
  }

  private _valueName(key: string): string {
    return`fh_value_${key}`;
  }

  private storageChangedListener(e: StorageEvent, repo: FeatureHubRepository) {
    let key: string = undefined;

    // ideal would be to have a post load event, find all the features
    // that have overridden states and track them as they change so
    // that we on a "clear" trigger the right listeners. This is MVP.
    if (e === undefined || e.key === undefined || e.key === null) {
      return;
    }

    if (repo === undefined) {
      console.log('repo is undefined?');
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
