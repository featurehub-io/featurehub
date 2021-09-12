import { FeatureHubRepository } from './featurehub_repository';

function createBaseBaggageHeader(header?: string) {
  let newHeader = '';

  if (header) {
    newHeader = header.split(',').filter(p => !p.startsWith('fhub')).join(',');
  }

  return newHeader;
}

function createBaggageHeader(features: string, newHeader: string) {
  if (features.length > 0) {
    if (newHeader.length > 0) {
      return newHeader + ',fhub=' + features;
    } else {
      return 'fhub=' + features;
    }
  } else if (newHeader.length > 0) {
    return newHeader;
  } else {
    return undefined;
  }
}

export interface BaggageHeader {
  repo?: FeatureHubRepository;
  values?: Map<string, string|undefined>;
  header?: string;
}

// allows for consistency between client and server
export function w3cBaggageHeader({ repo, values, header }: BaggageHeader): string|undefined {
  const newHeader = createBaseBaggageHeader(header);

  let features: string;
  if (values) {
    features = encodeURIComponent(
      Array.from(values)
        .map(e => e[0] + '=' + (e[1] ? encodeURIComponent(e[1]) : ''))
        .join(','));
  } else {
    features = encodeURIComponent(
      Array.from(repo.simpleFeatures().entries())
        .map(e => e[0] + '=' + (e[1] ? encodeURIComponent(e[1]) : ''))
        .join(','));
  }

  return createBaggageHeader(features, newHeader);
}
