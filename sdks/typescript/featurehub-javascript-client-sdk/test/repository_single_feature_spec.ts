import { ClientContext, ClientFeatureRepository, FeatureState, FeatureValueType, SSEResultState } from '../app';
import { expect } from 'chai';
import { Substitute } from '@fluffy-spoon/substitute';

describe('repository reacts to single feature changes as expected', () => {
  let repo: ClientFeatureRepository;

  beforeEach(() => {
    repo = new ClientFeatureRepository();
  });

  it('should be able to recognize newly created values that are not set', () => {
    const features = [
      { id: '1', key: 'pear', version: 0, type: FeatureValueType.String }
    ];

    repo.notify(SSEResultState.Features, features);

    expect(repo.feature('pear').getVersion()).to.eq(0);

    repo.notify(SSEResultState.Feature, { id: '1', key: 'pear', version: 1, type: FeatureValueType.String, value: 'now-set' });

    expect(repo.feature('pear').getVersion()).to.eq(1);
    expect(repo.feature('pear').version).to.eq(1);
    expect(repo.feature('pear').getString()).to.eq('now-set');
    expect(repo.feature('pear').str).to.eq('now-set');
  });

  it('should specify undefined for unknown feature values', () => {
    // tslint:disable-next-line:no-unused-expression
    expect(repo.getFeatureState('bool').getBoolean()).to.be.undefined;
    expect(repo.getFeatureState('bool').flag).to.be.undefined;
    // tslint:disable-next-line:no-unused-expression
    expect(repo.getFeatureState('num').getNumber()).to.be.undefined;
    expect(repo.getFeatureState('num').num).to.be.undefined;
    // tslint:disable-next-line:no-unused-expression
    expect(repo.getFeatureState('str').getString()).to.be.undefined;
    expect(repo.getFeatureState('str').str).to.be.undefined;
    // tslint:disable-next-line:no-unused-expression
    expect(repo.getFeatureState('str').getRawJson()).to.be.undefined;
    expect(repo.getFeatureState('str').rawJson).to.be.undefined;

    const ctx = Substitute.for<ClientContext>();
    const feat = repo.getFeatureState('bool').withContext(ctx);
    // tslint:disable-next-line:no-unused-expression
    expect(feat.flag).to.be.undefined;
  });

  it('should be able to deal with pure json data', () => {
    let triggerBanana = 0;
    let triggerPear = 0;
    let triggerPeach = 0;

    repo.getFeatureState('banana').addListener(() => triggerBanana++);
    repo.getFeatureState('pear').addListener(() => triggerPear++);
    repo.getFeatureState('peach').addListener(() => triggerPeach++);

    const features = [
      { id: '1', key: 'banana', version: 1, type: FeatureValueType.Json, value: '{}' },
      { id: '2', key: 'pear', version: 1, type: FeatureValueType.Json, value: '"nashi"' },
      { id: '3', key: 'peach', version: 1, type: FeatureValueType.Json, value: '{"variety": "golden queen"}' }
    ];

    repo.notify(SSEResultState.Features, features);

    repo.notify(SSEResultState.Feature, {
      id: '1', key: 'banana',
      version: 2, type: FeatureValueType.Json, value: '{}'
    });

    // banana doesn't change because version diff + value same
    expect(triggerBanana).to.eq(1);

    repo.notify(SSEResultState.Feature, {
      id: '1', key: 'banana',
      version: 3, type: FeatureValueType.Json, value: '"yellow"'
    });

    expect(triggerBanana).to.eq(2);
    expect(triggerPear).to.eq(1);
    expect(triggerPeach).to.eq(1);

    repo.notify(SSEResultState.DeleteFeature, { id: '1', key: 'banana' });

    expect(repo.hasFeature('banana').getRawJson()).to.be.undefined;
  });

  it('should react to a single feature changing', () => {
    let triggerBanana = 0;
    let triggerPear = 0;
    let triggerPeach = 0;

    repo.getFeatureState('banana').addListener(() => triggerBanana++);
    repo.getFeatureState('pear').addListener(() => triggerPear++);
    repo.getFeatureState('peach').addListener(() => triggerPeach++);

    const features = [
      new FeatureState({ id: '1', key: 'banana', version: 1, type: FeatureValueType.Json, value: '{}' }),
      new FeatureState({ id: '2', key: 'pear', version: 1, type: FeatureValueType.Json, value: '"nashi"' }),
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
      new FeatureState({ id: '1', key: 'apricot', version: 1, type: FeatureValueType.Number, value: 12.8 }),
    ];
    repo.notify(SSEResultState.Features, features);
    repo.notify(SSEResultState.Feature, new FeatureState(({ id: '1', key: 'apricot', version: 1,
      type: FeatureValueType.Number, value: 12.9 })));
    // expect(triggerApricot).to.eq(2);
    expect(repo.feature('apricot').getNumber()).to.eq(12.9);
    expect(repo.feature('apricot').num).to.eq(12.9);
  });
});
