import { InternalFeatureRepository } from './internal_feature_repository';

export class InterceptorValueMatch {
  public value: string | undefined;

  constructor(value: string) {
    this.value = value;
  }
}

export interface FeatureStateValueInterceptor {
  matched(key: string): InterceptorValueMatch;
  repository(repo: InternalFeatureRepository): void;
}
