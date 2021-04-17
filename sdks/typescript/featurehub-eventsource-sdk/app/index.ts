import { EdgeFeatureHubConfig } from 'featurehub-repository';
import { FeatureHubEventSourceClient } from './featurehub_eventsource';

EdgeFeatureHubConfig.defaultEdgeServiceSupplier = (repo, cfg) =>
  new FeatureHubEventSourceClient(cfg, repo);

export * from './featurehub_eventsource';
export * from 'featurehub-repository';
