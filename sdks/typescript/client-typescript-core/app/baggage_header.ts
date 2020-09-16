import { FeatureHubRepository } from './client_feature_repository';

export function w3cBaggageHeader(repo: FeatureHubRepository, header?: string): string|undefined {
  let newHeader = '';
  if (header) {
    newHeader = header.split(',').filter(p => !p.startsWith('fhub')).join(',');
  }

  const features = encodeURIComponent(
    Array.from(repo.simpleFeatures().entries())
      .map(e => {console.log(e); return e[0] + '=' + e[1] ? encodeURIComponent(e[1]) : '';})
      .join(','));

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
