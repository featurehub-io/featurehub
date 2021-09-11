import { FeatureHubRepository } from './featurehub_repository';
import { RolloutStrategy, SSEResultState } from './models';
import { InterceptorValueMatch } from './interceptors';
import { ClientContext } from './client_context';
import { Applied } from './strategy_matcher';

export interface InternalFeatureRepository extends FeatureHubRepository {

  // called when it is ready, but has changed important state (e.g. server eval and the client
  // change the context
  notReady(): void;

  notify(state: SSEResultState, data: any);

  valueInterceptorMatched(key: string): InterceptorValueMatch;

  apply(strategies: Array<RolloutStrategy>, key: string, featureValueId: string,
        context: ClientContext): Applied;
}
