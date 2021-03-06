import { FeatureHubRepository } from './client_feature_repository';
import { RolloutStrategy, SSEResultState } from './models/models';
import { InterceptorValueMatch } from './feature_state_holders';
import { ClientContext } from './client_context';
import { Applied } from './strategy_matcher';

export interface InternalFeatureRepository extends FeatureHubRepository {

  notReady(): void;

  notify(state: SSEResultState, data: any): void;

  valueInterceptorMatched(key: string): InterceptorValueMatch;

  apply(strategies: Array<RolloutStrategy>, key: string, featureValueId: string,
        context: ClientContext): Applied;
}
