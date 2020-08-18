import {
  ClientFeatureRepository,
  FeatureState,
  FeatureValueType,
  SSEResultState
} from '../app';
import { expect } from 'chai';

describe('Catch and release should hold and then release feature changes', () => {
  it('should enable me to turn on catch and no changes should flow and then i can release', () => {
    const repo = new ClientFeatureRepository();
    let postNewTrigger = 0;
    repo.addPostLoadNewFeatureStateAvailableListener(() => postNewTrigger ++);
    let bananaTrigger = 0;
    repo.getFeatureState('banana').addListener(() => bananaTrigger++ );
    expect(postNewTrigger).to.eq(0);
    expect(bananaTrigger).to.eq(0);

    repo.catchAndReleaseMode = true;

    expect(repo.catchAndReleaseMode).to.eq(true);

    const features = [
      new FeatureState({id: '1', key: 'banana', version: 1, type: FeatureValueType.BOOLEAN, value: true}),
    ];

    repo.notify(SSEResultState.Features, features);
    // change banana, change change banana
    repo.notify(SSEResultState.Feature, new FeatureState({id: '1', key: 'banana', version: 2,
      type: FeatureValueType.BOOLEAN, value: false}));
    expect(postNewTrigger).to.eq(1);
    expect(bananaTrigger).to.eq(1); // new list of features always trigger
    repo.notify(SSEResultState.Feature, new FeatureState({id: '1', key: 'banana', version: 3,
      type: FeatureValueType.BOOLEAN, value: false}));

    expect(postNewTrigger).to.eq(2);
    expect(bananaTrigger).to.eq(1);
    expect(repo.getFeatureState('banana').getBoolean()).to.eq(true);
    repo.release();
    expect(postNewTrigger).to.eq(2);
    expect(bananaTrigger).to.eq(2);
    expect(repo.getFeatureState('banana').getBoolean()).to.eq(false);

    // notify with new state, should still hold
    const features2 = [
      new FeatureState({id: '1', key: 'banana', version: 4, type: FeatureValueType.BOOLEAN, value: true}),
    ];

    repo.notify(SSEResultState.Features, features);
    expect(postNewTrigger).to.eq(2);
    expect(bananaTrigger).to.eq(2);
  });
});
