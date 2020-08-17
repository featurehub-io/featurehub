import { ClientFeatureRepository, FeatureState, FeatureValueType, Readyness, SSEResultState } from '../app';
import { expect } from 'chai';

describe('Feature repository reacts to incoming event lists as expected', () => {
  let repo: ClientFeatureRepository;

  beforeEach(() => {
    repo = new ClientFeatureRepository();
  });

  it('should accept a list of boolean features sets and triggers updates and stores values as expect', () => {
    let triggerBanana = 0;
    let triggerPear = 0;
    let triggerPeach = 0;

    repo.getFeatureState('banana').addListener(() => triggerBanana ++);
    repo.getFeatureState('pear').addListener(() => triggerPear ++);
    repo.getFeatureState('peach').addListener(() => triggerPeach ++);

    expect(triggerBanana).to.eq(0);
    expect(triggerPear).to.eq(0);
    expect(triggerPeach).to.eq(0);

    // tslint:disable-next-line:no-unused-expression
    expect(repo.getFeatureState('banana').getBoolean()).to.be.undefined;
    // tslint:disable-next-line:no-unused-expression
    expect(repo.getFeatureState('pear').getBoolean()).to.be.undefined;
    // tslint:disable-next-line:no-unused-expression
    expect(repo.getFeatureState('peach').getBoolean()).to.be.undefined;

    const features = [
      new FeatureState({id: '1', key: 'banana', version: 1, type: FeatureValueType.BOOLEAN, value: true}),
      new FeatureState({id: '2', key: 'pear', version: 1, type: FeatureValueType.BOOLEAN, value: false}),
      new FeatureState({id: '3', key: 'peach', version: 1, type: FeatureValueType.BOOLEAN, value: true}),
    ];

    repo.notify(SSEResultState.Features, features);

    expect(triggerBanana).to.eq(1);
    expect(triggerPear).to.eq(1);
    expect(triggerPeach).to.eq(1);
    // tslint:disable-next-line:no-unused-expression
    expect(repo.getFeatureState('banana').getBoolean()).to.be.true;
    // tslint:disable-next-line:no-unused-expression
    expect(repo.getFeatureState('pear').getBoolean()).to.be.false;
    // tslint:disable-next-line:no-unused-expression
    expect(repo.getFeatureState('peach').getBoolean()).to.be.true;

    const features2 = [
      new FeatureState({id: '1', key: 'banana', version: 2, type: FeatureValueType.BOOLEAN, value: false}),
      new FeatureState({id: '2', key: 'pear', version: 1, type: FeatureValueType.BOOLEAN, value: false}),
      new FeatureState({id: '3', key: 'peach', version: 2, type: FeatureValueType.BOOLEAN, value: true}),
    ];

    // only banana should trigger as it has changed its value and its version
    repo.notify(SSEResultState.Features, features2);

    expect(triggerBanana).to.eq(2);
    expect(triggerPear).to.eq(1);
    expect(triggerPeach).to.eq(1);
    // tslint:disable-next-line:no-unused-expression
    expect(repo.getFeatureState('banana').getBoolean()).to.be.false;
    // tslint:disable-next-line:no-unused-expression
    expect(repo.getFeatureState('pear').getBoolean()).to.be.false;
    // tslint:disable-next-line:no-unused-expression
    expect(repo.getFeatureState('peach').getBoolean()).to.be.true;
  });

  it('should accept a list of number features sets and triggers updates and stores values as expect', () => {
    let triggerBanana = 0;
    let triggerPear = 0;
    let triggerPeach = 0;

    repo.getFeatureState('banana').addListener(() => triggerBanana ++);
    repo.getFeatureState('pear').addListener(() => triggerPear ++);
    repo.getFeatureState('peach').addListener(() => triggerPeach ++);

    const features = [
      new FeatureState({id: '1', key: 'banana', version: 1, type: FeatureValueType.NUMBER, value: 7.2}),
      new FeatureState({id: '2', key: 'pear', version: 1, type: FeatureValueType.NUMBER, value: 15}),
      new FeatureState({id: '3', key: 'peach', version: 1, type: FeatureValueType.NUMBER, value: 56534.23}),
    ];

    repo.notify(SSEResultState.Features, features);

    expect(triggerBanana).to.eq(1);
    expect(triggerPear).to.eq(1);
    expect(triggerPeach).to.eq(1);
    expect(repo.getFeatureState('banana').getNumber()).to.eq(7.2);
    expect(repo.getFeatureState('pear').getNumber()).to.eq(15);
    expect(repo.getFeatureState('peach').getNumber()).to.eq(56534.23);

    const features2 = [
      new FeatureState({id: '1', key: 'banana', version: 2, type: FeatureValueType.NUMBER, value: 16}),
      new FeatureState({id: '2', key: 'pear', version: 1, type: FeatureValueType.NUMBER, value: 15}),
      new FeatureState({id: '3', key: 'peach', version: 2, type: FeatureValueType.NUMBER, value: 56534.23}),
    ];

    // only banana should trigger as it has changed its value and its version
    repo.notify(SSEResultState.Features, features2);

    expect(triggerBanana).to.eq(2);
    expect(triggerPear).to.eq(1);
    expect(triggerPeach).to.eq(1);
    expect(repo.getFeatureState('banana').getNumber()).to.eq(16);
    expect(repo.getFeatureState('pear').getNumber()).to.eq(15);
    expect(repo.getFeatureState('peach').getNumber()).to.eq(56534.23);
  });

  it('should accept a list of string features sets and triggers updates and stores values as expect', () => {
    let triggerBanana = 0;
    let triggerPear = 0;
    let triggerPeach = 0;

    repo.getFeatureState('banana').addListener(() => triggerBanana ++);
    repo.getFeatureState('pear').addListener(() => triggerPear ++);
    repo.getFeatureState('peach').addListener(() => triggerPeach ++);


    const features = [
      new FeatureState({id: '1', key: 'banana', version: 1, type: FeatureValueType.STRING, value: '7.2'}),
      new FeatureState({id: '2', key: 'pear', version: 1, type: FeatureValueType.STRING, value: '15'}),
      new FeatureState({id: '3', key: 'peach', version: 1, type: FeatureValueType.STRING, value: '56534.23'}),
    ];

    repo.notify(SSEResultState.Features, features);

    expect(triggerBanana).to.eq(1);
    expect(triggerPear).to.eq(1);
    expect(triggerPeach).to.eq(1);
    expect(repo.getFeatureState('banana').getString()).to.eq('7.2');
    expect(repo.getFeatureState('pear').getString()).to.eq('15');
    expect(repo.getFeatureState('peach').getString()).to.eq('56534.23');

    const features2 = [
      new FeatureState({id: '1', key: 'banana', version: 2, type: FeatureValueType.STRING, value: '16'}),
      new FeatureState({id: '2', key: 'pear', version: 1, type: FeatureValueType.STRING, value: '15'}),
      new FeatureState({id: '3', key: 'peach', version: 2, type: FeatureValueType.STRING, value: '56534.23'}),
    ];

    // only banana should trigger as it has changed its value and its version
    repo.notify(SSEResultState.Features, features2);

    expect(triggerBanana).to.eq(2);
    expect(triggerPear).to.eq(1);
    expect(triggerPeach).to.eq(1);
    expect(repo.getFeatureState('banana').getString()).to.eq('16');
    expect(repo.getFeatureState('pear').getString()).to.eq('15');
    expect(repo.getFeatureState('peach').getString()).to.eq('56534.23');
  });

  it('should accept a list of json features sets and triggers updates and stores values as expect', () => {
    let triggerBanana = 0;
    let triggerPear = 0;
    let triggerPeach = 0;

    repo.getFeatureState('banana').addListener(() => triggerBanana ++);
    repo.getFeatureState('pear').addListener(() => triggerPear ++);
    repo.getFeatureState('peach').addListener(() => triggerPeach ++);

    const features = [
      new FeatureState({id: '1', key: 'banana', version: 1, type: FeatureValueType.JSON, value: '{}'}),
      new FeatureState({id: '2', key: 'pear', version: 1, type: FeatureValueType.JSON, value: '"nashi"'}),
      new FeatureState({id: '3', key: 'peach', version: 1, type: FeatureValueType.JSON, value: '{"variety": "golden queen"}'}),
    ];

    repo.notify(SSEResultState.Features, features);

    expect(triggerBanana).to.eq(1);
    expect(triggerPear).to.eq(1);
    expect(triggerPeach).to.eq(1);
    expect(repo.getFeatureState('banana').getRawJson()).to.eq('{}');
    expect(repo.getFeatureState('pear').getRawJson()).to.eq('"nashi"');
    expect(repo.getFeatureState('peach').getRawJson()).to.eq('{"variety": "golden queen"}');

    const features2 = [
      new FeatureState({id: '1', key: 'banana', version: 2, type: FeatureValueType.JSON, value: '"yellow"'}),
      new FeatureState({id: '2', key: 'pear', version: 1, type: FeatureValueType.JSON, value: '"nashi"'}),
      new FeatureState({id: '3', key: 'peach', version: 2, type: FeatureValueType.JSON, value: '{"variety": "golden queen"}'}),
    ];

    // only banana should trigger as it has changed its value and its version
    repo.notify(SSEResultState.Features, features2);

    expect(triggerBanana).to.eq(2);
    expect(triggerPear).to.eq(1);
    expect(triggerPeach).to.eq(1);
    expect(repo.getFeatureState('banana').getRawJson()).to.eq('"yellow"');
    expect(repo.getFeatureState('pear').getRawJson()).to.eq('"nashi"');
    expect(repo.getFeatureState('peach').getRawJson()).to.eq('{"variety": "golden queen"}');
  });
});

describe('repository reacts to single feature changes as expected', () => {
  let repo: ClientFeatureRepository;

  beforeEach(() => {
    repo = new ClientFeatureRepository();
  });

  it('should react to a single feature changing', () => {
    let triggerBanana = 0;
    let triggerPear = 0;
    let triggerPeach = 0;

    repo.getFeatureState('banana').addListener(() => triggerBanana ++);
    repo.getFeatureState('pear').addListener(() => triggerPear ++);
    repo.getFeatureState('peach').addListener(() => triggerPeach ++);

    const features = [
      new FeatureState({id: '1', key: 'banana', version: 1, type: FeatureValueType.JSON, value: '{}'}),
      new FeatureState({id: '2', key: 'pear', version: 1, type: FeatureValueType.JSON, value: '"nashi"'}),
      new FeatureState({id: '3', key: 'peach', version: 1, type: FeatureValueType.JSON,
        value: '{"variety": "golden queen"}'}),
    ];

    repo.notify(SSEResultState.Features, features);

    repo.notify(SSEResultState.Feature, new FeatureState({id: '1', key: 'banana',
      version: 2, type: FeatureValueType.JSON, value: '{}'}));

    // banana doesn't change because version diff + value same
    expect(triggerBanana).to.eq(1);

    repo.notify(SSEResultState.Feature, new FeatureState({id: '1', key: 'banana',
      version: 3, type: FeatureValueType.JSON, value: '"yellow"'}));

    expect(triggerBanana).to.eq(2);
    expect(triggerPear).to.eq(1);
    expect(triggerPeach).to.eq(1);

  });
});

describe('Readyness listeners should fire on appropriate events', () => {
  it('should start not ready, receive a list of features and become ready and on failure be failed', () => {
    const repo = new ClientFeatureRepository();

    let readynessTrigger = 0;
    let lastReadyness: Readyness = undefined;
    repo.addReadynessListener((state) => {
      lastReadyness = state;
      return readynessTrigger++;
    });

    expect(repo.readyness).to.eq(Readyness.NotReady);
    expect(readynessTrigger).to.eq(1);

    const features = [
      new FeatureState({id: '1', key: 'banana', version: 1, type: FeatureValueType.BOOLEAN, value: true}),
    ];

    repo.notify(SSEResultState.Features, features);

    expect(repo.readyness).to.eq(Readyness.Ready);
    expect(lastReadyness).to.eq(Readyness.Ready);
    expect(readynessTrigger).to.eq(2);

    repo.notify(SSEResultState.Failure, null);
    expect(repo.readyness).to.eq(Readyness.Failed);
    expect(lastReadyness).to.eq(Readyness.Failed);
    expect(readynessTrigger).to.eq(3);
  });
});

describe('When any feature changes, post new feature update should trigger', () => {
  it('should not fire until first new feature and then should fire each new feature after that but only when new',
     () => {
    const repo = new ClientFeatureRepository();
    let postNewTrigger = 0;
    repo.addPostLoadNewFeatureStateAvailableListener(() => postNewTrigger ++);
    expect(postNewTrigger).to.eq(0);
    const features = [
     new FeatureState({id: '1', key: 'banana', version: 1, type: FeatureValueType.BOOLEAN, value: true}),
    ];

    repo.notify(SSEResultState.Features, features);
    expect(postNewTrigger).to.eq(0);

    repo.notify(SSEResultState.Feature, new FeatureState({id: '1', key: 'banana', version: 2,
        type: FeatureValueType.BOOLEAN, value: true}));

    expect(postNewTrigger).to.eq(0);
    repo.notify(SSEResultState.Feature, new FeatureState({id: '1', key: 'banana', version: 3,
     type: FeatureValueType.BOOLEAN, value: false}));

    expect(postNewTrigger).to.eq(1);
  });
});
