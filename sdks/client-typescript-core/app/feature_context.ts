import { ClientFeatureRepository } from './client_feature_repository';

// create a default one, but they can replace it if they wish
export let featureHubRepository = new ClientFeatureRepository();

export class FeatureContext {
  /**
   * isActive is only valid for feature flags.
   *
   * @param flagKey - the key of the feature flag
   */
  static isActive(flagKey: string): boolean {
    return featureHubRepository.getFeatureState(flagKey).getBoolean() ?? false;
  }

  static exists(key: string): boolean {
    return featureHubRepository.getFeatureState(key).getKey() !== undefined;
  }

  static isSet(key: string): boolean {
    return featureHubRepository.getFeatureState(key).isSet();
  }

  static logAnalyticsEvent(action: string, other?: Map<string, string>): void {
    featureHubRepository.logAnalyticsEvent(action, other);
  }
}
