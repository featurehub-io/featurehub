import { ClientFeatureRepository, FeatureState, FeatureValueType, SSEResultState } from '../app';
import { expect } from 'chai';

describe('repository reacts to single feature changes as expected', () => {
  let repo: ClientFeatureRepository;

  beforeEach(() => {
    repo = new ClientFeatureRepository();
  });

  it('should react to a single feature changing', () => {
    let triggerBanana = 0;
    let triggerPear = 0;
    let triggerPeach = 0;

    repo.getFeatureState('banana').addListener(() => triggerBanana++);
    repo.getFeatureState('pear').addListener(() => triggerPear++);
    repo.getFeatureState('peach').addListener(() => triggerPeach++);

    const features = [
      new FeatureState({id: '1', key: 'banana', version: 1, type: FeatureValueType.Json, value: '{}'}),
      new FeatureState({id: '2', key: 'pear', version: 1, type: FeatureValueType.Json, value: '"nashi"'}),
      new FeatureState({
        id: '3', key: 'peach', version: 1, type: FeatureValueType.Json,
        value: '{"variety": "golden queen"}'
      }),
    ];

    repo.notify(SSEResultState.Features, features);

    repo.notify(SSEResultState.Feature, new FeatureState({
      id: '1', key: 'banana',
      version: 2, type: FeatureValueType.Json, value: '{}'
    }));

    // banana doesn't change because version diff + value same
    expect(triggerBanana).to.eq(1);

    repo.notify(SSEResultState.Feature, new FeatureState({
      id: '1', key: 'banana',
      version: 3, type: FeatureValueType.Json, value: '"yellow"'
    }));

    expect(triggerBanana).to.eq(2);
    expect(triggerPear).to.eq(1);
    expect(triggerPeach).to.eq(1);

  });

  it('should react when the version stays the same but the value changes', () => {
    let triggerApricot = 0;
    repo.feature('apricot').addListener(() => triggerApricot++);
    const features = [
      new FeatureState({id: '1', key: 'apricot', version: 1, type: FeatureValueType.Number, value: 12.8}),
    ];
    repo.notify(SSEResultState.Features, features);
    repo.notify(SSEResultState.Feature, new FeatureState(({id: '1', key: 'apricot', version: 1,
      type: FeatureValueType.Number, value: 12.9})));
    // expect(triggerApricot).to.eq(2);
    expect(repo.feature('apricot').getNumber()).to.eq(12.9);
  });
});
