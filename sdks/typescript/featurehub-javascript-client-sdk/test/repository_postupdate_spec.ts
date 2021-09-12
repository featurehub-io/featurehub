import {
  ClientFeatureRepository,
  FeatureState,
  FeatureValueType,
  SSEResultState
} from '../app';
import { expect } from 'chai';

describe('When any feature changes, post new feature update should trigger', () => {
  it('should not fire until first new feature and then should fire each new feature after that but only when new',
    () => {
      const repo = new ClientFeatureRepository();
      let postNewTrigger = 0;
      repo.addPostLoadNewFeatureStateAvailableListener(() => postNewTrigger++);
      expect(postNewTrigger).to.eq(0);
      const features = [
        new FeatureState({ id: '1', key: 'banana', version: 1, type: FeatureValueType.Boolean, value: true }),
      ];

      repo.notify(SSEResultState.Features, features);
      expect(postNewTrigger).to.eq(0);

      repo.notify(SSEResultState.Feature, new FeatureState({ id: '1', key: 'banana', version: 2,
        type: FeatureValueType.Boolean, value: true }));

      expect(postNewTrigger).to.eq(0);
      repo.notify(SSEResultState.Feature, new FeatureState({ id: '1', key: 'banana', version: 3,
        type: FeatureValueType.Boolean, value: false }));

      expect(postNewTrigger).to.eq(1);
    });
});
