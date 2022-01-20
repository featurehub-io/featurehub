import {
  ClientFeatureRepository,
  EdgeFeatureHubConfig,
  EdgeService,
  FeatureState,
  FeatureValueType,
  RolloutStrategy,
  RolloutStrategyAttribute,
  RolloutStrategyAttributeConditional,
  RolloutStrategyFieldType,
  SSEResultState
} from '../app';
import { expect } from 'chai';
import { Substitute } from '@fluffy-spoon/substitute';

describe('Feature repository reacts to incoming event lists as expected', () => {
  let repo: ClientFeatureRepository;

  beforeEach(() => {
    repo = new ClientFeatureRepository();
  });

  it('Can handle null or undefined feature states', () => {
    repo.notify(SSEResultState.Features, [undefined]);
  });

  it('Can handle post new state available handlers failing and letting subsequent ones continue', () => {
    let postTrigger = 0;
    let failTrigger = 0;
    repo.addPostLoadNewFeatureStateAvailableListener(() => {
      failTrigger++;
      throw new Error('blah');
    });
    repo.addPostLoadNewFeatureStateAvailableListener(() => postTrigger++);
    const features = [
      new FeatureState({ id: '1', key: 'banana', version: 1, type: FeatureValueType.Boolean, value: true }),
    ];

    repo.notify(SSEResultState.Features, features);
    repo.notify(SSEResultState.Feature, new FeatureState({
      id: '1', key: 'banana', version: 2,
      type: FeatureValueType.Boolean, value: false
    }));

    expect(postTrigger).to.eq(1);
    expect(failTrigger).to.eq(1);
  });

  it('should accept a list of boolean features sets and triggers updates and stores values as expect', () => {
    let triggerBanana = 0;
    let triggerPear = 0;
    let triggerPeach = 0;

    repo.getFeatureState('banana').addListener(() => triggerBanana++);
    repo.getFeatureState('pear').addListener(() => triggerPear++);
    repo.getFeatureState('peach').addListener(() => triggerPeach++);

    expect(triggerBanana).to.eq(0);
    expect(triggerPear).to.eq(0);
    expect(triggerPeach).to.eq(0);

    // tslint:disable-next-line:no-unused-expression
    expect(repo.getFeatureState('banana').getBoolean()).to.be.undefined;
    // tslint:disable-next-line:no-unused-expression
    expect(repo.getFeatureState('banana').isSet()).to.be.false;
    // tslint:disable-next-line:no-unused-expression
    expect(repo.getFeatureState('pear').getBoolean()).to.be.undefined;
    // tslint:disable-next-line:no-unused-expression
    expect(repo.getFeatureState('peach').getBoolean()).to.be.undefined;

    const features = [
      new FeatureState({ id: '1', key: 'banana', version: 1, type: FeatureValueType.Boolean, value: true }),
      new FeatureState({ id: '2', key: 'pear', version: 1, type: FeatureValueType.Boolean, value: false }),
      new FeatureState({ id: '3', key: 'peach', version: 1, type: FeatureValueType.Boolean, value: true }),
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
      new FeatureState({ id: '1', key: 'banana', version: 2, type: FeatureValueType.Boolean, value: false }),
      new FeatureState({ id: '2', key: 'pear', version: 1, type: FeatureValueType.Boolean, value: false }),
      new FeatureState({ id: '3', key: 'peach', version: 2, type: FeatureValueType.Boolean, value: true }),
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

    repo.getFeatureState('banana').addListener(() => triggerBanana++);
    repo.getFeatureState('pear').addListener(() => triggerPear++);
    repo.getFeatureState('peach').addListener(() => triggerPeach++);

    const features = [
      new FeatureState({ id: '1', key: 'banana', version: 1, type: FeatureValueType.Number, value: 7.2 }),
      new FeatureState({ id: '2', key: 'pear', version: 1, type: FeatureValueType.Number, value: 15 }),
      new FeatureState({ id: '3', key: 'peach', version: 1, type: FeatureValueType.Number, value: 56534.23 }),
    ];

    repo.notify(SSEResultState.Features, features);

    expect(triggerBanana).to.eq(1);
    expect(triggerPear).to.eq(1);
    expect(triggerPeach).to.eq(1);
    expect(repo.getFeatureState('banana').getNumber()).to.eq(7.2);
    expect(repo.getFeatureState('pear').getNumber()).to.eq(15);
    expect(repo.getFeatureState('peach').getNumber()).to.eq(56534.23);

    const features2 = [
      new FeatureState({ id: '1', key: 'banana', version: 2, type: FeatureValueType.Number, value: 16 }),
      new FeatureState({ id: '2', key: 'pear', version: 1, type: FeatureValueType.Number, value: 15 }),
      new FeatureState({ id: '3', key: 'peach', version: 2, type: FeatureValueType.Number, value: 56534.23 }),
    ];

    // only banana should trigger as it has changed its value and its version
    repo.notify(SSEResultState.Features, features2);

    expect(triggerBanana).to.eq(2);
    expect(triggerPear).to.eq(1);
    expect(triggerPeach).to.eq(1);
    expect(repo.getFeatureState('banana').getNumber()).to.eq(16);
    expect(repo.getFeatureState('pear').getNumber()).to.eq(15);
    expect(repo.getFeatureState('peach').getNumber()).to.eq(56534.23);
    expect(repo.getNumber('pear')).to.eq(15);
    expect(repo.isSet('pear')).to.eq(true);
  });

  it('should accept and trigger events via a context and the repo in the same fashion for the same feature',
    async () => {
      const fhConfig = new EdgeFeatureHubConfig('http://localhost:8080', '123*123');
      fhConfig.repository(repo);
      const edgeService = Substitute.for<EdgeService>();
      fhConfig.edgeServiceProvider(() => edgeService);

      let triggerContext = 0;
      let triggerRepo = 0;
      let contextNumber = 0;
      let repoNumber = 0;

      repo.getFeatureState('fruit').addListener((fs) => {
        repoNumber = fs.getNumber();
        triggerRepo++;
      });
      const fhContext = await fhConfig.newContext().userKey('fred').build();
      const feat = fhContext.feature('fruit');
      feat.addListener((fs) => {
        contextNumber = fs.getNumber();
        triggerContext++;
      });

      const features = [
        new FeatureState({
          id: '1', key: 'fruit', version: 2, type: FeatureValueType.Number, value: 16,
          strategies: [new RolloutStrategy({
            id: '1', value: 12, attributes: [new RolloutStrategyAttribute({
              type: RolloutStrategyFieldType.String, fieldName: 'userkey', values: ['fred'],
              conditional: RolloutStrategyAttributeConditional.Equals
            })]
          })]
        }),
      ];

      // only banana should trigger as it has changed its value and its version
      repo.notify(SSEResultState.Features, features);

      expect(triggerContext).to.eq(1);
      expect(triggerRepo).to.eq(1);
      expect(repoNumber).to.eq(16);
      expect(contextNumber).to.eq(12);

      // mimic the same revision coming through, but a different main value, which is what would happen for a server
      // eval change
      const features2 = [
        new FeatureState({
          id: '1', key: 'fruit', version: 2, type: FeatureValueType.Number, value: 17,
          strategies: [new RolloutStrategy({
            id: '1', value: 13, attributes: [new RolloutStrategyAttribute({
              type: RolloutStrategyFieldType.String, fieldName: 'userkey', values: ['fred'],
              conditional: RolloutStrategyAttributeConditional.Equals
            })]
          })]
        }),
      ];

      repo.notify(SSEResultState.Features, features2);

      expect(triggerContext).to.eq(2);
      expect(triggerRepo).to.eq(2);
      expect(repoNumber).to.eq(17);
      expect(contextNumber).to.eq(13);
    });

  it('should accept a list of string features sets and triggers updates and stores values as expect', () => {
    let triggerBanana = 0;
    let triggerPear = 0;
    let triggerPeach = 0;

    repo.getFeatureState('banana').addListener(() => triggerBanana++);
    repo.getFeatureState('pear').addListener(() => triggerPear++);
    repo.getFeatureState('peach').addListener(() => triggerPeach++);

    const features = [
      new FeatureState({ id: '1', key: 'banana', version: 1, type: FeatureValueType.String, value: '7.2' }),
      new FeatureState({ id: '2', key: 'pear', version: 1, type: FeatureValueType.String, value: '15' }),
      new FeatureState({ id: '3', key: 'peach', version: 1, type: FeatureValueType.String, value: '56534.23' }),
    ];

    repo.notify(SSEResultState.Features, features);

    expect(triggerBanana).to.eq(1);
    expect(triggerPear).to.eq(1);
    expect(triggerPeach).to.eq(1);
    expect(repo.getFeatureState('banana').getString()).to.eq('7.2');
    expect(repo.feature('banana').str).to.eq('7.2');
    expect(repo.getFeatureState('pear').getString()).to.eq('15');
    expect(repo.feature('pear').str).to.eq('15');
    expect(repo.getFeatureState('peach').getString()).to.eq('56534.23');

    const features2 = [
      new FeatureState({ id: '1', key: 'banana', version: 2, type: FeatureValueType.String, value: '16' }),
      new FeatureState({ id: '2', key: 'pear', version: 1, type: FeatureValueType.String, value: '15' }),
      new FeatureState({ id: '3', key: 'peach', version: 2, type: FeatureValueType.String, value: '56534.23' }),
    ];

    // only banana should trigger as it has changed its value and its version
    repo.notify(SSEResultState.Features, features2);

    expect(triggerBanana).to.eq(2);
    expect(triggerPear).to.eq(1);
    expect(triggerPeach).to.eq(1);
    expect(repo.getFeatureState('banana').getString()).to.eq('16');
    expect(repo.getFeatureState('pear').getString()).to.eq('15');
    expect(repo.getFeatureState('peach').getString()).to.eq('56534.23');
    expect(repo.getString('peach')).to.eq('56534.23');
  });

  it('should accept a list of json features sets and triggers updates and stores values as expect', () => {
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
        id: '3',
        key: 'peach',
        version: 1,
        type: FeatureValueType.Json,
        value: '{"variety": "golden queen"}'
      }),
    ];

    repo.notify(SSEResultState.Features, features);

    expect(triggerBanana).to.eq(1);
    expect(triggerPear).to.eq(1);
    expect(triggerPeach).to.eq(1);
    expect(repo.getFeatureState('banana').getRawJson()).to.eq('{}');
    expect(repo.getFeatureState('pear').getRawJson()).to.eq('"nashi"');
    expect(repo.getFeatureState('peach').getRawJson()).to.eq('{"variety": "golden queen"}');

    const features2 = [
      new FeatureState({ id: '1', key: 'banana', version: 2, type: FeatureValueType.Json, value: '"yellow"' }),
      new FeatureState({ id: '2', key: 'pear', version: 1, type: FeatureValueType.Json, value: '"nashi"' }),
      new FeatureState({
        id: '3',
        key: 'peach',
        version: 2,
        type: FeatureValueType.Json,
        value: '{"variety": "golden queen"}'
      }),
    ];

    // only banana should trigger as it has changed its value and its version
    repo.notify(SSEResultState.Features, features2);

    expect(triggerBanana).to.eq(2);
    expect(triggerPear).to.eq(1);
    expect(triggerPeach).to.eq(1);
    expect(repo.getFeatureState('banana').getRawJson()).to.eq('"yellow"');
    expect(repo.getFeatureState('pear').getRawJson()).to.eq('"nashi"');
    expect(repo.getFeatureState('peach').getRawJson()).to.eq('{"variety": "golden queen"}');
    expect(repo.getJson('pear')).to.eq('"nashi"');
    expect(repo.isSet('pear')).to.eq(true);
  });
});
