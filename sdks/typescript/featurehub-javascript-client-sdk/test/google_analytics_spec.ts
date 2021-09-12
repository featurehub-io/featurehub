import {
  ClientFeatureRepository,
  FeatureState,
  FeatureValueType,
  GoogleAnalyticsApiClient,
  GoogleAnalyticsCollector, SSEResultState
} from '../app';
import { expect } from 'chai';

describe('Google analytics collector should output correct info', () => {
  it('we should provide all different types of features and the analytics should log all except JSON', async () => {
    const cid = 'cid';
    let postedData: string = undefined;

    const apiClient = {
      cid: function (other: Map<string, string>) {
        return other.get('cid');
      },

      postBatchUpdate(batchData: string) {
        postedData = batchData;
      }
    } as GoogleAnalyticsApiClient;

    const ga = new GoogleAnalyticsCollector('UA-123', cid, apiClient);

    const repo = new ClientFeatureRepository();
    repo.addAnalyticCollector(ga);

    const features = [
      new FeatureState({ id: '1', key: 'banana', version: 1, type: FeatureValueType.Boolean, value: true }),
      new FeatureState({ id: '2', key: 'pear', version: 1, type: FeatureValueType.Number, value: 12.3 }),
      new FeatureState({ id: '3', key: 'peach', version: 1, type: FeatureValueType.String, value: 'golden' }),
      new FeatureState({ id: '4', key: 'cherimoya', version: 1, type: FeatureValueType.Json, value: '"custard apple"' }),
    ];

    repo.notify(SSEResultState.Features, features);

    await repo.logAnalyticsEvent('action');

    expect(postedData).to.eq(`v=1&tid=UA-123&cid=cid&t=event&ec=FeatureHub%20Event&ea=action&el=banana%20:%20on
v=1&tid=UA-123&cid=cid&t=event&ec=FeatureHub%20Event&ea=action&el=pear%20:%2012.3
v=1&tid=UA-123&cid=cid&t=event&ec=FeatureHub%20Event&ea=action&el=peach%20:%20golden
`);
  });
});
